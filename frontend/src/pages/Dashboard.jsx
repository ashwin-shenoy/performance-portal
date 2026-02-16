import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Grid,
  Column,
  Tile,
  ClickableTile,
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
  Loading,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListBody,
  StructuredListRow,
  StructuredListCell,
} from '@carbon/react';
import {
  Dashboard as DashboardIcon,
  Upload,
  DocumentBlank,
  Rocket,
  Flash,
  CheckmarkFilled,
  View,
  Edit,
} from '@carbon/icons-react';
import { LineChart } from '@carbon/charts-react';
import '@carbon/charts-react/styles.css';
import dayjs from 'dayjs';
import axios from '../utils/axios';
import { API_ENDPOINTS } from '../config/api';

const Dashboard = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    totalTests: 0,
    completedTests: 0,
    totalReports: 0,
  });
  const [trendData, setTrendData] = useState([]);
  const [recentTests, setRecentTests] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const summaryResponse = await axios.get(API_ENDPOINTS.ANALYTICS_SUMMARY);
      setStats(summaryResponse.data);

      const trendsResponse = await axios.get(API_ENDPOINTS.ANALYTICS_TRENDS);
      setTrendData(trendsResponse.data);

      // Fetch recent test runs
      const testsResponse = await axios.get(API_ENDPOINTS.TESTS);
      setRecentTests(testsResponse.data.slice(0, 10)); // Get latest 10 tests
    } catch (err) {
      console.error('Error fetching dashboard data:', err);
      
      // Use sample data
      setStats({
        totalTests: 45,
        completedTests: 38,
        totalReports: 12,
      });

      setTrendData([
        { group: 'Tests', date: 'Jan', value: 12 },
        { group: 'Tests', date: 'Feb', value: 18 },
        { group: 'Tests', date: 'Mar', value: 15 },
        { group: 'Tests', date: 'Apr', value: 22 },
        { group: 'Tests', date: 'May', value: 28 },
        { group: 'Tests', date: 'Jun', value: 25 },
      ]);
      setRecentTests([]);
    } finally {
      setLoading(false);
    }
  };

  const chartOptions = {
    title: 'Test Execution Trends',
    axes: {
      bottom: {
        title: 'Month',
        mapsTo: 'date',
        scaleType: 'labels',
      },
      left: {
        mapsTo: 'value',
        title: 'Number of Tests',
        scaleType: 'linear',
      },
    },
    curve: 'curveMonotoneX',
    height: '300px',
    color: {
      scale: {
        Tests: '#000000',
      },
    },
  };

  const testColumns = [
    {
      key: 'testName',
      header: 'Test Name',
    },
    {
      key: 'capability',
      header: 'Capability',
    },
    {
      key: 'testType',
      header: 'Test Type',
    },
    {
      key: 'status',
      header: 'Status',
    },
    {
      key: 'uploadDate',
      header: 'Upload Date',
    },
    {
      key: 'actions',
      header: 'Actions',
    },
  ];

  const getTestTypeTag = (testType) => {
    return <Tag type="gray">JMeter</Tag>;
  };

  const getStatusTag = (status) => {
    const statusMap = {
      'COMPLETED': 'green',
      'PROCESSING': 'gray',
      'FAILED': 'red',
    };
    return <Tag type={statusMap[status] || 'gray'}>{status}</Tag>;
  };

  const testRows = recentTests.map((test) => ({
    id: test.id,
    testName: test.testName,
    capability: <Tag type="gray">{test.capability}</Tag>,
    testType: getTestTypeTag(test.testType),
    status: getStatusTag(test.status),
    uploadDate: dayjs(test.uploadDate).format('YYYY-MM-DD HH:mm'),
    actions: (
      <Button
        kind="ghost"
        size="sm"
        renderIcon={View}
        onClick={() => navigate('/tests')}
      >
        View
      </Button>
    ),
  }));

  const actionCards = [
    {
      key: 'dashboard',
      title: 'Performance Dashboard',
      description: 'View all performance test results, metrics, and analytics in one place',
      icon: DashboardIcon,
      path: '/dashboard',
      stats: `${stats.totalTests} Tests | ${stats.completedTests} Completed`,
      action: 'View Dashboard',
      color: '#000000',
    },
    {
      key: 'upload',
      title: 'Upload Test Results',
      description: 'Upload JTL files with JMeter test data for analysis',
      icon: Upload,
      path: '/upload',
      stats: 'JTL files supported',
      action: 'Upload Files',
      color: '#24a148',
    },
    {
      key: 'reports',
      title: 'Report Generation',
      description: 'Generate professional Word reports from your performance test data',
      icon: DocumentBlank,
      path: '/reports',
      stats: `${stats.totalReports} Reports Generated`,
      action: 'Generate Report',
      color: '#8a3ffc',
    },
    {
      key: 'capability-details',
      title: 'Capability Details',
      description: 'Add narrative content that powers report generation',
      icon: Edit,
      path: '/capability-details',
      stats: `${stats.totalTests} Test Runs Ready`,
      action: 'Add Details',
      color: '#0f62fe',
    },
  ];

  return (
    <div className="page-container fade-in">
      {/* Hero Section */}
      <Tile style={{ padding: '2rem', marginBottom: '2rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '2rem' }}>
          <Rocket size={40} />
          <div>
            <h1 style={{ margin: 0, fontSize: '2rem', fontWeight: 400 }}>
              Performance Test Portal
            </h1>
            <p style={{ margin: '0.5rem 0 0 0', fontSize: '0.875rem', color: '#525252' }}>
              Manage and generate performance test reports
            </p>
          </div>
        </div>

        <Grid narrow>
          <Column sm={4} md={4} lg={4}>
            <Tile style={{ padding: '1.5rem', textAlign: 'center', background: '#f4f4f4' }}>
              <Flash size={32} style={{ marginBottom: '0.75rem' }} />
              <div style={{ fontSize: '2rem', fontWeight: '300' }}>{stats.totalTests}</div>
              <div style={{ fontSize: '0.875rem', color: '#525252' }}>Total Tests</div>
            </Tile>
          </Column>
          <Column sm={4} md={4} lg={4}>
            <Tile style={{ padding: '1.5rem', textAlign: 'center', background: '#f4f4f4' }}>
              <CheckmarkFilled size={32} style={{ marginBottom: '0.75rem' }} />
              <div style={{ fontSize: '2rem', fontWeight: '300' }}>{stats.completedTests}</div>
              <div style={{ fontSize: '0.875rem', color: '#525252' }}>Completed</div>
            </Tile>
          </Column>
          <Column sm={4} md={4} lg={4}>
            <Tile style={{ padding: '1.5rem', textAlign: 'center', background: '#f4f4f4' }}>
              <DocumentBlank size={32} style={{ marginBottom: '0.75rem' }} />
              <div style={{ fontSize: '2rem', fontWeight: '300' }}>{stats.totalReports}</div>
              <div style={{ fontSize: '0.875rem', color: '#525252' }}>Reports</div>
            </Tile>
          </Column>
        </Grid>
      </Tile>

      {/* Quick Actions */}
      <h3 style={{ marginBottom: '1.5rem', fontSize: '1.125rem', fontWeight: '400' }}>Quick Actions</h3>
      <Grid narrow>
        {actionCards.map((card) => {
          const IconComponent = card.icon;
          return (
            <Column key={card.key} sm={4} md={4} lg={4}>
              <ClickableTile
                onClick={() => navigate(card.path)}
                style={{ minHeight: '280px', padding: '2rem' }}
              >
                <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
                  <IconComponent size={40} />
                </div>
                <h4 style={{ margin: '0 0 0.5rem 0', fontSize: '1rem', fontWeight: '500' }}>{card.title}</h4>
                <p style={{ color: '#525252', margin: '0 0 1rem 0', fontSize: '0.875rem', lineHeight: '1.5' }}>
                  {card.description}
                </p>
                <Button kind="primary" size="sm" style={{ width: '100%' }}>
                  {card.action}
                </Button>
              </ClickableTile>
            </Column>
          );
        })}
      </Grid>

      {/* Recent Test Results */}
      <div style={{ marginTop: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h4 style={{ margin: 0, fontSize: '1.125rem', fontWeight: '400' }}>Recent Test Results</h4>
          <Button kind="ghost" onClick={() => navigate('/tests')}>
            View All Tests →
          </Button>
        </div>
        <Tile>
          {loading ? (
            <Loading description="Loading test results..." withOverlay={false} />
          ) : (
            <DataTable rows={testRows} headers={testColumns}>
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
      </div>

      {/* Test Trends Chart */}
      <div style={{ marginTop: '2rem' }}>
        <h4 style={{ margin: '0 0 1rem 0', fontSize: '1.125rem', fontWeight: '400' }}>Test Execution Trends</h4>
        <Tile>
          <LineChart data={trendData} options={chartOptions} />
        </Tile>
      </div>
    </div>
  );
};

export default Dashboard;

// Made with Bob
