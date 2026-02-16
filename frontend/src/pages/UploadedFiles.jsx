import { useState, useEffect } from 'react';
import {
  Grid,
  Column,
  Tile,
  Button,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  Search,
  Select,
  SelectItem,
  Modal,
  Loading,
  InlineNotification,
  StructuredListWrapper,
  StructuredListBody,
  StructuredListRow,
  StructuredListCell,
} from '@carbon/react';
import {
  FolderOpen,
  View,
  TrashCan,
  Renew,
  DocumentBlank,
} from '@carbon/icons-react';
import dayjs from 'dayjs';
import axios from '../utils/axios';
import { API_ENDPOINTS } from '../config/api';

const UploadedFiles = () => {
  const [loading, setLoading] = useState(false);
  const [testRuns, setTestRuns] = useState([]);
  const [filteredData, setFilteredData] = useState([]);
  const [selectedTest, setSelectedTest] = useState(null);
  const [detailsVisible, setDetailsVisible] = useState(false);
  const [filterCapability, setFilterCapability] = useState('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [notification, setNotification] = useState(null);
  const [generatingReport, setGeneratingReport] = useState(null);

  useEffect(() => {
    fetchTestRuns();
  }, []);

  useEffect(() => {
    filterTests();
  }, [testRuns, filterCapability, searchTerm]);

  const showNotification = (kind, title, subtitle) => {
    setNotification({ kind, title, subtitle });
    setTimeout(() => setNotification(null), 5000);
  };

  const fetchTestRuns = async () => {
    setLoading(true);
    try {
      const response = await axios.get(API_ENDPOINTS.TESTS);
      setTestRuns(response.data);
    } catch (error) {
      console.error('Error fetching test runs:', error);
      showNotification('error', 'Failed to fetch files', error.response?.data?.message || error.message);
      setTestRuns([]);
    } finally {
      setLoading(false);
    }
  };

  const filterTests = () => {
    let filtered = testRuns;
    
    if (filterCapability !== 'all') {
      filtered = filtered.filter(test => test.capability === filterCapability);
    }
    
    if (searchTerm) {
      filtered = filtered.filter(test =>
        test.testName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        test.capability?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        test.fileName?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }
    
    setFilteredData(filtered);
  };

  const handleViewDetails = (record) => {
    setSelectedTest(record);
    setDetailsVisible(true);
  };

  const handleGenerateReport = async (record) => {
    try {
      setGeneratingReport(record.id);
      showNotification('info', 'Generating report...', 'Please wait while we generate your report');

      try {
        await axios.get(API_ENDPOINTS.REPORT_VALIDATE_JMETER(record.id));
      } catch (validationError) {
        const missing = validationError.response?.data?.missingFields || [];
        showNotification(
          'error',
          'Missing required fields',
          missing.length > 0 ? missing.join(', ') : 'Cover page fields are incomplete'
        );
        return;
      }

      const endpoint = API_ENDPOINTS.GENERATE_WORD_REPORT(record.id);
      
      const response = await axios.post(endpoint);
      showNotification('success', 'Report generated', 'Report has been generated successfully');
      
      if (response.data.reportId) {
        setTimeout(() => {
          window.location.href = API_ENDPOINTS.REPORT_DOWNLOAD(response.data.reportId);
        }, 1000);
      }
    } catch (error) {
      console.error('Error generating report:', error);
      showNotification('error', 'Report generation failed', error.response?.data?.message || error.message);
    } finally {
      setGeneratingReport(null);
    }
  };

  const headers = [
    { key: 'testName', header: 'Test Name' },
    { key: 'capability', header: 'Capability' },
    { key: 'testType', header: 'Test Type' },
    { key: 'buildNumber', header: 'Build Number' },
    { key: 'uploadDate', header: 'Upload Date' },
    { key: 'status', header: 'Status' },
    { key: 'actions', header: 'Actions' },
  ];

  const getTestTypeTag = (testType) => {
    const typeMap = {
      'JMETER': { type: 'gray', label: 'JMeter' },
    };
    const type = typeMap[testType] || { type: 'gray', label: testType || 'Unknown' };
    return <Tag type={type.type}>{type.label}</Tag>;
  };

  const getStatusTag = (status) => {
    const statusMap = {
      'COMPLETED': 'green',
      'PROCESSING': 'gray',
      'FAILED': 'red',
    };
    return <Tag type={statusMap[status] || 'gray'}>{status}</Tag>;
  };

  const rows = filteredData.map((test) => ({
    id: test.id,
    testName: test.testName || 'N/A',
    capability: <Tag type="gray">{test.capability || 'N/A'}</Tag>,
    testType: getTestTypeTag(test.testType),
    buildNumber: test.buildNumber || 'N/A',
    uploadDate: test.uploadDate ? dayjs(test.uploadDate).format('YYYY-MM-DD HH:mm') : 'N/A',
    status: getStatusTag(test.status),
    actions: (
      <div style={{ display: 'flex', gap: '0.5rem' }}>
        <Button
          kind="ghost"
          size="sm"
          renderIcon={View}
          onClick={() => handleViewDetails(test)}
          hasIconOnly
          iconDescription="View Details"
        />
        <Button
          kind="primary"
          size="sm"
          renderIcon={DocumentBlank}
          onClick={() => handleGenerateReport(test)}
          disabled={generatingReport === test.id}
          hasIconOnly
          iconDescription="Generate Report"
        />
      </div>
    ),
  }));

  // Get unique capabilities for filters
  const capabilities = ['all', ...new Set(testRuns.map(t => t.capability).filter(Boolean))];
  return (
    <div className="page-container fade-in">
      {/* Hero Section */}
      <Tile
        style={{
          background: 'linear-gradient(135deg, #000000 0%, #262626 100%)',
          padding: '3rem',
          marginBottom: '2rem',
          color: 'white',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <FolderOpen size={48} />
          <div>
            <h1 style={{ margin: 0, fontSize: '2.25rem', fontWeight: 600 }}>
              Uploaded Files
            </h1>
            <p style={{ margin: '0.5rem 0 0 0', fontSize: '1rem', opacity: 0.9 }}>
              View and manage your uploaded test results
            </p>
          </div>
        </div>
      </Tile>

      {/* Notifications */}
      {notification && (
        <div style={{ marginBottom: '1.5rem' }}>
          <InlineNotification
            kind={notification.kind}
            title={notification.title}
            subtitle={notification.subtitle}
            onClose={() => setNotification(null)}
            lowContrast
          />
        </div>
      )}

      {/* Filters */}
      <Tile style={{ marginBottom: '1.5rem' }}>
        <Grid narrow>
          <Column sm={4} md={4} lg={6}>
            <Search
              labelText="Search"
              placeholder="Search by test name, capability, or file name"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              onClear={() => setSearchTerm('')}
            />
          </Column>
          <Column sm={4} md={2} lg={3}>
            <Select
              id="capability-filter"
              labelText="Capability"
              value={filterCapability}
              onChange={(e) => setFilterCapability(e.target.value)}
            >
              {capabilities.map((cap) => (
                <SelectItem key={cap} value={cap} text={cap === 'all' ? 'All Capabilities' : cap} />
              ))}
            </Select>
          </Column>
          <Column sm={4} md={2} lg={4}>
            <div style={{ marginTop: '1.75rem' }}>
              <Button kind="secondary" renderIcon={Renew} onClick={fetchTestRuns}>
                Refresh
              </Button>
            </div>
          </Column>
        </Grid>
      </Tile>

      {/* Files Table */}
      <Tile>
        <div style={{ marginBottom: '1rem' }}>
          <h4 style={{ margin: 0, fontSize: '1.25rem' }}>
            Test Files ({filteredData.length} of {testRuns.length})
          </h4>
        </div>
        {loading ? (
          <Loading description="Loading test files..." withOverlay={false} />
        ) : filteredData.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem' }}>
            <FolderOpen size={64} style={{ opacity: 0.3, marginBottom: '1rem' }} />
            <h5>No files found</h5>
            <p style={{ color: '#525252' }}>
              {testRuns.length === 0 ? 'Upload test results to see them here' : 'Try adjusting your filters'}
            </p>
          </div>
        ) : (
          <DataTable rows={rows} headers={headers}>
            {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
              <TableContainer>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
                        <TableHeader {...getHeaderProps({ header })} key={header.key}>
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => (
                      <TableRow {...getRowProps({ row })} key={row.id}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        )}
      </Tile>

      {/* Details Modal */}
      <Modal
        open={detailsVisible}
        onRequestClose={() => setDetailsVisible(false)}
        modalHeading="Test Details"
        passiveModal
        size="lg"
      >
        {selectedTest && (
          <StructuredListWrapper>
            <StructuredListBody>
              <StructuredListRow>
                <StructuredListCell head>Test Name</StructuredListCell>
                <StructuredListCell>{selectedTest.testName}</StructuredListCell>
              </StructuredListRow>
              <StructuredListRow>
                <StructuredListCell head>Capability</StructuredListCell>
                <StructuredListCell>{selectedTest.capability}</StructuredListCell>
              </StructuredListRow>
              <StructuredListRow>
                <StructuredListCell head>Test Type</StructuredListCell>
                <StructuredListCell>{getTestTypeTag(selectedTest.testType)}</StructuredListCell>
              </StructuredListRow>
              <StructuredListRow>
                <StructuredListCell head>Build Number</StructuredListCell>
                <StructuredListCell>{selectedTest.buildNumber}</StructuredListCell>
              </StructuredListRow>
              <StructuredListRow>
                <StructuredListCell head>Upload Date</StructuredListCell>
                <StructuredListCell>
                  {selectedTest.uploadDate ? dayjs(selectedTest.uploadDate).format('YYYY-MM-DD HH:mm:ss') : 'N/A'}
                </StructuredListCell>
              </StructuredListRow>
              <StructuredListRow>
                <StructuredListCell head>Status</StructuredListCell>
                <StructuredListCell>{getStatusTag(selectedTest.status)}</StructuredListCell>
              </StructuredListRow>
              {selectedTest.description && (
                <StructuredListRow>
                  <StructuredListCell head>Description</StructuredListCell>
                  <StructuredListCell>{selectedTest.description}</StructuredListCell>
                </StructuredListRow>
              )}
            </StructuredListBody>
          </StructuredListWrapper>
        )}
      </Modal>
    </div>
  );
};

export default UploadedFiles;

// Made with Bob