import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
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
  Modal,
  Loading,
  InlineNotification,
} from '@carbon/react';
import {
  DocumentBlank,
  Download,
  View,
  TrashCan,
  Renew,
  Add,
  DocumentPdf,
} from '@carbon/icons-react';
import dayjs from 'dayjs';
import axios from '../utils/axios';
import { API_ENDPOINTS } from '../config/api';

const Reports = () => {
  const navigate = useNavigate();
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedReport, setSelectedReport] = useState(null);
  const [detailsVisible, setDetailsVisible] = useState(false);
  const [notification, setNotification] = useState(null);

  useEffect(() => {
    fetchReports();
  }, []);

  const showNotification = (kind, title, subtitle) => {
    setNotification({ kind, title, subtitle });
    setTimeout(() => setNotification(null), 5000);
  };

  const fetchReports = async () => {
    setLoading(true);
    try {
      let response;
      try {
        response = await axios.get(API_ENDPOINTS.REPORTS_LIST);
      } catch (err) {
        console.log('Trying alternative endpoint...');
        response = await axios.get(API_ENDPOINTS.REPORTS);
      }
      
      if (Array.isArray(response.data)) {
        setReports(response.data);
      } else if (response.data.reports && Array.isArray(response.data.reports)) {
        setReports(response.data.reports);
      } else if (response.data.data && Array.isArray(response.data.data)) {
        setReports(response.data.data);
      } else {
        console.log('No reports found in response:', response.data);
        setReports([]);
      }
    } catch (error) {
      console.error('Error fetching reports:', error);
      showNotification('error', 'Failed to load reports', error.response?.data?.message || error.message);
      setReports([]);
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async (reportId, fileName) => {
    try {
      showNotification('info', 'Downloading report...', 'Please wait');
      window.location.href = API_ENDPOINTS.REPORT_DOWNLOAD(reportId);
      
      setTimeout(() => {
        showNotification('success', 'Download started', 'Report download has begun');
      }, 500);
    } catch (error) {
      console.error('Error downloading report:', error);
      showNotification('error', 'Download failed', 'Failed to download report');
    }
  };

  const handleDelete = async (reportId) => {
    setSelectedReport(reportId);
    setDetailsVisible(true);
  };

  const confirmDelete = async () => {
    try {
      await axios.delete(API_ENDPOINTS.DELETE_REPORT(selectedReport));
      showNotification('success', 'Report deleted', 'Report deleted successfully');
      setDetailsVisible(false);
      fetchReports();
    } catch (error) {
      console.error('Error deleting report:', error);
      showNotification('error', 'Delete failed', 'Failed to delete report');
    }
  };

  const headers = [
    { key: 'fileName', header: 'File Name' },
    { key: 'capability', header: 'Capability' },
    { key: 'testType', header: 'Test Type' },
    { key: 'generatedDate', header: 'Generated Date' },
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

  const rows = reports.map((report) => ({
    id: report.reportId || report.id,
    fileName: report.fileName || 'N/A',
    capability: <Tag type="gray">{report.capability || 'N/A'}</Tag>,
    testType: getTestTypeTag(report.testType || report.reportType),
    generatedDate: report.generatedAt ? dayjs(report.generatedAt).format('YYYY-MM-DD HH:mm') : (report.generatedDate ? dayjs(report.generatedDate).format('YYYY-MM-DD HH:mm') : 'N/A'),
    status: <Tag type={report.status === 'COMPLETED' ? 'green' : 'gray'}>{report.status || 'COMPLETED'}</Tag>,
    actions: (
      <div style={{ display: 'flex', gap: '0.5rem' }}>
        <Button
          kind="ghost"
          size="sm"
          renderIcon={Download}
          onClick={() => handleDownload(report.reportId || report.id, report.fileName)}
          hasIconOnly
          iconDescription="Download"
        />
        <Button
          kind="danger--ghost"
          size="sm"
          renderIcon={TrashCan}
          onClick={() => handleDelete(report.reportId || report.id)}
          hasIconOnly
          iconDescription="Delete"
        />
      </div>
    ),
  }));

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
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem' }}>
          <DocumentBlank size={48} />
          <div>
            <h1 style={{ margin: 0, fontSize: '2.25rem', fontWeight: 600 }}>
              Generated Reports
            </h1>
            <p style={{ margin: '0.5rem 0 0 0', fontSize: '1rem', opacity: 0.9 }}>
              View and download your performance test reports
            </p>
          </div>
        </div>
        <div style={{ display: 'flex', gap: '1rem' }}>
          <Button
            kind="tertiary"
            renderIcon={Add}
            onClick={() => navigate('/upload')}
          >
            Upload New Test
          </Button>
          <Button
            kind="tertiary"
            renderIcon={Renew}
            onClick={fetchReports}
          >
            Refresh
          </Button>
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

      {/* Reports Table */}
      <Tile>
        <div style={{ marginBottom: '1rem' }}>
          <h4 style={{ margin: 0, fontSize: '1.25rem' }}>All Reports ({reports.length})</h4>
        </div>
        {loading ? (
          <Loading description="Loading reports..." withOverlay={false} />
        ) : reports.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem' }}>
            <DocumentPdf size={64} style={{ opacity: 0.3, marginBottom: '1rem' }} />
            <h5>No reports found</h5>
            <p style={{ color: '#525252', marginBottom: '1.5rem' }}>
              Upload test results and generate reports to see them here
            </p>
            <Button kind="primary" renderIcon={Add} onClick={() => navigate('/upload')}>
              Upload Test Results
            </Button>
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

      {/* Delete Confirmation Modal */}
      <Modal
        open={detailsVisible}
        onRequestClose={() => setDetailsVisible(false)}
        onRequestSubmit={confirmDelete}
        modalHeading="Delete Report"
        primaryButtonText="Delete"
        secondaryButtonText="Cancel"
        danger
      >
        <p>Are you sure you want to delete this report? This action cannot be undone.</p>
      </Modal>
    </div>
  );
};

export default Reports;

// Made with Bob
