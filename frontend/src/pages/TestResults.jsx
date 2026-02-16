import { useState, useEffect } from 'react';
import { Table, Card, Typography, Space, Button, Tag, Input, Select, Modal, Descriptions, message, Dropdown } from 'antd';
import { SearchOutlined, EyeOutlined, DeleteOutlined, DownloadOutlined, FileTextOutlined, FilePdfOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import axios from '../utils/axios';
import { API_ENDPOINTS } from '../config/api';

const { Title } = Typography;
const { Search } = Input;

const TestResults = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [testRuns, setTestRuns] = useState([]);
  const [filteredData, setFilteredData] = useState([]);
  const [selectedTest, setSelectedTest] = useState(null);
  const [detailsVisible, setDetailsVisible] = useState(false);
  const [filterCapability, setFilterCapability] = useState('all');
  const [generatingReport, setGeneratingReport] = useState({});

  useEffect(() => {
    fetchTestRuns();
  }, []);

  useEffect(() => {
    filterTests();
  }, [testRuns, filterCapability]);

  const fetchTestRuns = async () => {
    setLoading(true);
    try {
      const response = await axios.get(API_ENDPOINTS.TESTS);
      setTestRuns(response.data);
    } catch (error) {
      console.error('Error fetching test runs:', error);
      message.error('Failed to fetch test runs: ' + (error.response?.data?.message || error.message));
      setTestRuns([]);
    } finally {
      setLoading(false);
    }
  };

  const filterTests = () => {
    let filtered = testRuns;
    if (filterCapability !== 'all') {
      filtered = testRuns.filter(test => test.capability === filterCapability);
    }
    setFilteredData(filtered);
  };

  const handleSearch = (value) => {
    const filtered = testRuns.filter(test =>
      test.testName.toLowerCase().includes(value.toLowerCase()) ||
      test.capability.toLowerCase().includes(value.toLowerCase())
    );
    setFilteredData(filtered);
  };

  const handleViewDetails = (record) => {
    setSelectedTest(record);
    setDetailsVisible(true);
  };

  const handleDelete = async (id) => {
    Modal.confirm({
      title: 'Delete Test Run',
      content: 'Are you sure you want to delete this test run? This action cannot be undone.',
      okText: 'Delete',
      okType: 'danger',
      onOk: async () => {
        try {
          await axios.delete(API_ENDPOINTS.TEST_BY_ID(id));
          fetchTestRuns();
        } catch (error) {
          console.error('Error deleting test run:', error);
        }
      },
    });
  };

  const handleGenerateReport = async (testRunId) => {
    setGeneratingReport(prev => ({ ...prev, [testRunId]: true }));
    try {
      try {
        await axios.get(API_ENDPOINTS.REPORT_VALIDATE_JMETER(testRunId));
      } catch (validationError) {
        const missing = validationError.response?.data?.missingFields || [];
        message.error(`Missing required fields: ${missing.join(', ') || 'Cover page fields are incomplete'}`);
        return;
      }

      const endpoint = API_ENDPOINTS.GENERATE_PDF_REPORT(testRunId);
      
      const response = await axios.post(endpoint);
      
      if (response.data.success) {
        message.success('Report generated successfully!');
        const reportId = response.data.data.reportId;
        
        // Auto-download the report
        window.location.href = API_ENDPOINTS.REPORT_DOWNLOAD(reportId);
      }
    } catch (error) {
      console.error('Error generating report:', error);
      message.error('Failed to generate report: ' + (error.response?.data?.message || error.message));
    } finally {
      setGeneratingReport(prev => ({ ...prev, [testRunId]: false }));
    }
  };

  const handleGenerateWordReport = (testRunId) => {
    // Navigate to Report Builder for Word report customization
    navigate(`/report-builder/${testRunId}`);
  };

  const handleDownloadReport = async (testRunId) => {
    try {
      const response = await axios.get(API_ENDPOINTS.REPORT_STATUS(testRunId));
      
      if (response.data.hasReport && response.data.latestReport) {
        const reportId = response.data.latestReport.reportId;
        window.location.href = API_ENDPOINTS.REPORT_DOWNLOAD(reportId);
        message.success('Downloading report...');
      } else {
        message.info('No reports available for this test run. Generate one first.');
      }
    } catch (error) {
      console.error('Error downloading report:', error);
      message.error('Failed to download report');
    }
  };

  const capabilityFilters = Array.from(
    new Set(testRuns.map((test) => test.capability).filter(Boolean))
  ).map((capability) => ({ text: capability, value: capability }));

  const columns = [
    {
      title: 'Test Name',
      dataIndex: 'testName',
      key: 'testName',
      sorter: (a, b) => a.testName.localeCompare(b.testName),
    },
    {
      title: 'Capability',
      dataIndex: 'capability',
      key: 'capability',
      filters: capabilityFilters,
      onFilter: (value, record) => record.capability === value,
    },
    {
      title: 'Test Type',
      dataIndex: 'testType',
      key: 'testType',
      render: (testType) => {
        const typeMap = {
          'JMETER': { color: 'default', label: 'JMeter' },
        };
        const type = typeMap[testType] || { color: 'default', label: testType || 'Unknown' };
        return <Tag color={type.color}>{type.label}</Tag>;
      },
      filters: [
        { text: 'JMeter', value: 'JMETER' },
      ],
      onFilter: (value, record) => record.testType === value,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const color = status === 'COMPLETED' ? 'green' : status === 'PROCESSING' ? 'default' : 'red';
        return <Tag color={color}>{status}</Tag>;
      },
    },
    {
      title: 'Upload Date',
      dataIndex: 'uploadDate',
      key: 'uploadDate',
      render: (date) => dayjs(date).format('YYYY-MM-DD HH:mm'),
      sorter: (a, b) => dayjs(a.uploadDate).unix() - dayjs(b.uploadDate).unix(),
    },
    {
      title: 'Transactions',
      dataIndex: 'totalTransactions',
      key: 'totalTransactions',
      render: (value) => value?.toLocaleString() || 'N/A',
      sorter: (a, b) => (a.totalTransactions || 0) - (b.totalTransactions || 0),
    },
    {
      title: 'Avg Response (ms)',
      dataIndex: 'avgResponseTime',
      key: 'avgResponseTime',
      render: (value) => value?.toFixed(2) || 'N/A',
      sorter: (a, b) => (a.avgResponseTime || 0) - (b.avgResponseTime || 0),
    },
    {
      title: 'Error Rate (%)',
      dataIndex: 'errorRate',
      key: 'errorRate',
      render: (value) => {
        const color = value > 5 ? 'red' : value > 2 ? 'orange' : 'green';
        return <span style={{ color }}>{value?.toFixed(2) || 'N/A'}%</span>;
      },
      sorter: (a, b) => (a.errorRate || 0) - (b.errorRate || 0),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 450,
      render: (_, record) => {
        return (
          <Space wrap size="small">
            <Button
              type="link"
              icon={<EyeOutlined />}
              onClick={() => handleViewDetails(record)}
              size="small"
            >
              View
            </Button>
            
            <Dropdown
              menu={{
                items: [
                  {
                    key: 'download-latest',
                    label: 'Download Latest',
                    icon: <DownloadOutlined />,
                    onClick: () => handleDownloadReport(record.id),
                    disabled: record.status !== 'COMPLETED',
                  },
                  {
                    key: 'generate-pdf',
                    label: 'Generate PDF',
                    icon: <FilePdfOutlined />,
                    onClick: () => handleGenerateReport(record.id),
                    disabled: record.status !== 'COMPLETED',
                  },
                  {
                    key: 'customize',
                    label: 'Customize Report',
                    icon: <FileTextOutlined />,
                    onClick: () => handleGenerateWordReport(record.id),
                    disabled: record.status !== 'COMPLETED',
                  },
                ],
              }}
              trigger={['click']}
              disabled={record.status !== 'COMPLETED'}
            >
              <Button
                type="primary"
                icon={<DownloadOutlined />}
                size="small"
                disabled={record.status !== 'COMPLETED'}
              >
                Reports
              </Button>
            </Dropdown>
            >
              Delete
            </Button>
          </Space>
        );
      },
    },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Title level={2}>Test Results</Title>

      <Card>
        <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }}>
          <Space>
            <Search
              placeholder="Search tests..."
              onSearch={handleSearch}
              style={{ width: 300 }}
              prefix={<SearchOutlined />}
            />
            <Select
              defaultValue="all"
              style={{ width: 200 }}
              onChange={setFilterCapability}
              options={[
                { value: 'all', label: 'All Capabilities' },
                { value: 'API Performance', label: 'API Performance' },
                { value: 'Integration', label: 'Integration' },
                { value: 'Messaging', label: 'Messaging' },
                { value: 'Database', label: 'Database' },
                { value: 'Microservices', label: 'Microservices' },
              ]}
            />
          </Space>
          <Button type="primary" onClick={fetchTestRuns}>
            Refresh
          </Button>
        </Space>

        <Table
          columns={columns}
          dataSource={filteredData}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `Total ${total} tests`,
          }}
        />
      </Card>

      <Modal
        title="Test Run Details"
        open={detailsVisible}
        onCancel={() => setDetailsVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailsVisible(false)}>
            Close
          </Button>,
        ]}
        width={800}
      >
        {selectedTest && (
          <Descriptions bordered column={2}>
            <Descriptions.Item label="Test Name" span={2}>
              {selectedTest.testName}
            </Descriptions.Item>
            <Descriptions.Item label="Capability">
              {selectedTest.capability}
            </Descriptions.Item>
            <Descriptions.Item label="Test Type">
              {(() => {
                const typeMap = {
                  'JMETER': { color: 'default', label: 'JMeter' },
                };
                const type = typeMap[selectedTest.testType] || { color: 'default', label: selectedTest.testType || 'Unknown' };
                return <Tag color={type.color}>{type.label}</Tag>;
              })()}
            </Descriptions.Item>
            <Descriptions.Item label="Status">
              <Tag color={selectedTest.status === 'COMPLETED' ? 'green' : 'default'}>
                {selectedTest.status}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Upload Date">
              {dayjs(selectedTest.uploadDate).format('YYYY-MM-DD HH:mm:ss')}
            </Descriptions.Item>
            <Descriptions.Item label="Total Transactions">
              {selectedTest.totalTransactions?.toLocaleString()}
            </Descriptions.Item>
            <Descriptions.Item label="Virtual Users">
              {selectedTest.virtualUsers || 'N/A'}
            </Descriptions.Item>
            <Descriptions.Item label="Avg Response Time">
              {selectedTest.avgResponseTime?.toFixed(2)} ms
            </Descriptions.Item>
            <Descriptions.Item label="Error Rate">
              {selectedTest.errorRate?.toFixed(2)}%
            </Descriptions.Item>
            <Descriptions.Item label="Throughput">
              {selectedTest.throughput?.toFixed(2)} req/sec
            </Descriptions.Item>
            <Descriptions.Item label="Test Duration">
              {selectedTest.testDuration ? `${selectedTest.testDuration} seconds` : 'N/A'}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </Space>
  );
};

export default TestResults;

// Made with Bob
