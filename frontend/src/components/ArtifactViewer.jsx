import { useState, useEffect } from 'react';
import { Card, Image, Button, Space, Typography, Spin, Alert, Modal, Table, Descriptions, Tag } from 'antd';
import { DownloadOutlined, EyeOutlined, FileImageOutlined, FileExcelOutlined, ZoomInOutlined, ZoomOutOutlined } from '@ant-design/icons';
import axios from '../utils/axios';
import { API_ENDPOINTS } from '../config/api';

const { Title, Text } = Typography;

/**
 * Comprehensive Artifact Viewer Component
 * Displays architecture diagrams and test cases with preview, zoom, and download functionality
 */
const ArtifactViewer = ({ testRunId }) => {
  const [loading, setLoading] = useState(true);
  const [architectureDiagram, setArchitectureDiagram] = useState(null);
  const [testCasesSummary, setTestCasesSummary] = useState(null);
  const [testCases, setTestCases] = useState([]);
  const [testCaseStats, setTestCaseStats] = useState(null);
  const [error, setError] = useState(null);
  const [previewVisible, setPreviewVisible] = useState(false);
  const [previewImage, setPreviewImage] = useState('');

  useEffect(() => {
    if (testRunId) {
      fetchArtifacts();
    }
  }, [testRunId]);

  const fetchArtifacts = async () => {
    setLoading(true);
    setError(null);

    try {
      // Fetch architecture diagram
      try {
        const diagramResponse = await axios.get(
          `${API_ENDPOINTS.ARTIFACTS}/test-run/${testRunId}/architecture-diagram`
        );
        setArchitectureDiagram(diagramResponse.data);
      } catch (err) {
        if (err.response?.status !== 404) {
          console.error('Error fetching architecture diagram:', err);
        }
      }

      // Fetch test cases summary artifact
      try {
        const summaryResponse = await axios.get(
          `${API_ENDPOINTS.ARTIFACTS}/test-run/${testRunId}/test-cases-summary`
        );
        setTestCasesSummary(summaryResponse.data);
      } catch (err) {
        if (err.response?.status !== 404) {
          console.error('Error fetching test cases summary:', err);
        }
      }

      // Fetch test cases data
      try {
        const testCasesResponse = await axios.get(
          `${API_ENDPOINTS.TEST_CASES}?testRunId=${testRunId}`
        );
        setTestCases(testCasesResponse.data);

        // Fetch test case statistics
        const statsResponse = await axios.get(
          `${API_ENDPOINTS.TEST_CASES}/test-run/${testRunId}/statistics`
        );
        setTestCaseStats(statsResponse.data);
      } catch (err) {
        if (err.response?.status !== 404) {
          console.error('Error fetching test cases:', err);
        }
      }
    } catch (err) {
      setError('Failed to load artifacts. Please try again.');
      console.error('Error fetching artifacts:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async (artifactId, fileName) => {
    try {
      const response = await axios.get(
        `${API_ENDPOINTS.ARTIFACTS}/${artifactId}/download`,
        { responseType: 'blob' }
      );

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Error downloading artifact:', err);
      Alert.error('Failed to download file');
    }
  };

  const handlePreview = (artifactId) => {
    setPreviewImage(`${API_ENDPOINTS.ARTIFACTS}/${artifactId}/preview`);
    setPreviewVisible(true);
  };

  const testCaseColumns = [
    {
      title: 'Test Case ID',
      dataIndex: 'testCaseId',
      key: 'testCaseId',
      width: 120,
    },
    {
      title: 'Scenario Name',
      dataIndex: 'scenarioName',
      key: 'scenarioName',
      width: 200,
    },
    {
      title: 'Test Case Name',
      dataIndex: 'testCaseName',
      key: 'testCaseName',
      width: 200,
    },
    {
      title: 'Category',
      dataIndex: 'category',
      key: 'category',
      width: 120,
    },
    {
      title: 'Priority',
      dataIndex: 'priority',
      key: 'priority',
      width: 100,
      render: (priority) => {
        const colors = { HIGH: 'red', MEDIUM: 'orange', LOW: 'blue' };
        return <Tag color={colors[priority] || 'default'}>{priority}</Tag>;
      },
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => {
        const colors = {
          PASS: 'success',
          FAIL: 'error',
          SKIP: 'warning',
          BLOCKED: 'default',
          PENDING: 'processing',
        };
        return <Tag color={colors[status] || 'default'}>{status}</Tag>;
      },
    },
    {
      title: 'Infrastructure Tool',
      dataIndex: 'infrastructureTool',
      key: 'infrastructureTool',
      width: 150,
    },
    {
      title: 'CI/CD Tool',
      dataIndex: 'cicdTool',
      key: 'cicdTool',
      width: 120,
    },
    {
      title: 'Execution Time',
      dataIndex: 'executionTime',
      key: 'executionTime',
      width: 120,
      render: (time) => (time ? `${time} ms` : 'N/A'),
    },
  ];

  if (loading) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '40px' }}>
          <Spin size="large" />
          <div style={{ marginTop: 16 }}>
            <Text>Loading artifacts...</Text>
          </div>
        </div>
      </Card>
    );
  }

  if (error) {
    return (
      <Alert
        message="Error"
        description={error}
        type="error"
        showIcon
        action={
          <Button size="small" onClick={fetchArtifacts}>
            Retry
          </Button>
        }
      />
    );
  }

  const hasAnyArtifacts = architectureDiagram || testCasesSummary || testCases.length > 0;

  if (!hasAnyArtifacts) {
    return (
      <Alert
        message="No Artifacts Available"
        description="No architecture diagrams or test cases have been uploaded for this test run."
        type="info"
        showIcon
      />
    );
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      {/* Architecture Diagram Section */}
      {architectureDiagram && (
        <Card
          title={
            <Space>
              <FileImageOutlined style={{ color: '#1890ff' }} />
              <Text strong>Architecture Diagram</Text>
            </Space>
          }
          extra={
            <Space>
              <Button
                icon={<EyeOutlined />}
                onClick={() => handlePreview(architectureDiagram.id)}
              >
                Preview
              </Button>
              <Button
                icon={<DownloadOutlined />}
                onClick={() => handleDownload(architectureDiagram.id, architectureDiagram.fileName)}
              >
                Download
              </Button>
            </Space>
          }
        >
          <Descriptions column={2} size="small">
            <Descriptions.Item label="File Name">{architectureDiagram.fileName}</Descriptions.Item>
            <Descriptions.Item label="File Type">{architectureDiagram.fileType.toUpperCase()}</Descriptions.Item>
            <Descriptions.Item label="File Size">
              {(architectureDiagram.fileSize / 1024).toFixed(2)} KB
            </Descriptions.Item>
            <Descriptions.Item label="Uploaded At">
              {new Date(architectureDiagram.uploadedAt).toLocaleString()}
            </Descriptions.Item>
          </Descriptions>

          <div style={{ marginTop: 16, textAlign: 'center' }}>
            <Image
              src={architectureDiagram.previewUrl}
              alt="Architecture Diagram"
              style={{ maxWidth: '100%', maxHeight: '400px' }}
              preview={{
                mask: (
                  <Space>
                    <ZoomInOutlined /> Preview
                  </Space>
                ),
              }}
            />
          </div>
        </Card>
      )}

      {/* Test Cases Summary Section */}
      {testCasesSummary && (
        <Card
          title={
            <Space>
              <FileExcelOutlined style={{ color: '#52c41a' }} />
              <Text strong>Test Cases Summary File</Text>
            </Space>
          }
          extra={
            <Button
              icon={<DownloadOutlined />}
              onClick={() => handleDownload(testCasesSummary.id, testCasesSummary.fileName)}
            >
              Download
            </Button>
          }
        >
          <Descriptions column={2} size="small">
            <Descriptions.Item label="File Name">{testCasesSummary.fileName}</Descriptions.Item>
            <Descriptions.Item label="File Type">{testCasesSummary.fileType.toUpperCase()}</Descriptions.Item>
            <Descriptions.Item label="File Size">
              {(testCasesSummary.fileSize / 1024).toFixed(2)} KB
            </Descriptions.Item>
            <Descriptions.Item label="Uploaded At">
              {new Date(testCasesSummary.uploadedAt).toLocaleString()}
            </Descriptions.Item>
          </Descriptions>
        </Card>
      )}

      {/* Test Cases Statistics */}
      {testCaseStats && (
        <Card
          title={
            <Space>
              <FileExcelOutlined style={{ color: '#722ed1' }} />
              <Text strong>Test Cases Statistics</Text>
            </Space>
          }
        >
          <Descriptions column={3} bordered size="small">
            <Descriptions.Item label="Total Test Cases">{testCaseStats.total}</Descriptions.Item>
            <Descriptions.Item label="Passed">
              <Tag color="success">{testCaseStats.passed}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Failed">
              <Tag color="error">{testCaseStats.failed}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Skipped">
              <Tag color="warning">{testCaseStats.skipped}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Blocked">
              <Tag color="default">{testCaseStats.blocked}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Pass Rate">
              <Tag color={testCaseStats.passRate >= 80 ? 'success' : 'warning'}>
                {testCaseStats.passRate}%
              </Tag>
            </Descriptions.Item>
          </Descriptions>

          {testCaseStats.categories && Object.keys(testCaseStats.categories).length > 0 && (
            <div style={{ marginTop: 16 }}>
              <Text strong>Category Breakdown:</Text>
              <div style={{ marginTop: 8 }}>
                {Object.entries(testCaseStats.categories).map(([category, count]) => (
                  <Tag key={category} style={{ margin: '4px' }}>
                    {category}: {count}
                  </Tag>
                ))}
              </div>
            </div>
          )}
        </Card>
      )}

      {/* Test Cases Table */}
      {testCases.length > 0 && (
        <Card
          title={
            <Space>
              <FileExcelOutlined style={{ color: '#13c2c2' }} />
              <Text strong>Test Cases Details ({testCases.length})</Text>
            </Space>
          }
        >
          <Table
            columns={testCaseColumns}
            dataSource={testCases}
            rowKey="id"
            scroll={{ x: 1500 }}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `Total ${total} test cases`,
            }}
            size="small"
          />
        </Card>
      )}

      {/* Image Preview Modal */}
      <Modal
        open={previewVisible}
        title="Architecture Diagram Preview"
        footer={null}
        onCancel={() => setPreviewVisible(false)}
        width="90%"
        style={{ top: 20 }}
      >
        <div style={{ textAlign: 'center' }}>
          <img
            src={previewImage}
            alt="Architecture Diagram"
            style={{ maxWidth: '100%', maxHeight: '80vh' }}
          />
        </div>
      </Modal>
    </Space>
  );
};

export default ArtifactViewer;

// Made with Bob