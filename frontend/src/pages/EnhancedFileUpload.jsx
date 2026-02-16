import { useEffect, useState } from 'react';
import { Upload, Button, Card, Typography, Space, message, Progress, Select, Form, Alert, Divider, Row, Col } from 'antd';
import { InboxOutlined, UploadOutlined, FileImageOutlined, FileExcelOutlined } from '@ant-design/icons';
import axios from '../utils/axios';
import { API_ENDPOINTS } from '../config/api';

const { Title, Text } = Typography;
const { Dragger } = Upload;

const EnhancedFileUpload = () => {
  const [performanceFile, setPerformanceFile] = useState(null);
  const [architectureFile, setArchitectureFile] = useState(null);
  const [testCasesFile, setTestCasesFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [capabilities, setCapabilities] = useState([]);
  const [loadingCapabilities, setLoadingCapabilities] = useState(true);
  const [form] = Form.useForm();

  useEffect(() => {
    const fetchCapabilities = async () => {
      try {
        setLoadingCapabilities(true);
        const response = await axios.get(API_ENDPOINTS.CAPABILITIES_DROPDOWN);
        setCapabilities(response.data || []);
      } catch (error) {
        console.error('Error fetching capabilities:', error);
        message.error('Failed to load capabilities');
        setCapabilities([]);
      } finally {
        setLoadingCapabilities(false);
      }
    };

    fetchCapabilities();
  }, []);

  const capabilityOptions = capabilities.map((cap) => ({
    value: cap.value,
    label: cap.label,
  }));

  // Performance Data Upload Props
  const performanceUploadProps = {
    name: 'performanceData',
    maxCount: 1,
    beforeUpload: (file) => {
      const isValidType = 
        file.name.endsWith('.jtl') || 
        file.name.endsWith('.csv') || 
        file.name.endsWith('.xlsx') ||
        file.name.endsWith('.xls');
      
      if (!isValidType) {
        message.error('Performance data: Only JTL, CSV, or Excel files allowed!');
        return Upload.LIST_IGNORE;
      }

      const isLt100M = file.size / 1024 / 1024 < 100;
      if (!isLt100M) {
        message.error('Performance data file must be smaller than 100MB!');
        return Upload.LIST_IGNORE;
      }

      setPerformanceFile(file);
      return false;
    },
    onRemove: () => {
      setPerformanceFile(null);
    },
    fileList: performanceFile ? [performanceFile] : [],
  };

  // Architecture Diagram Upload Props
  const architectureUploadProps = {
    name: 'architectureDiagram',
    maxCount: 1,
    accept: 'image/png,image/jpeg,image/jpg,image/svg+xml',
    beforeUpload: (file) => {
      const isValidType = 
        file.type === 'image/png' || 
        file.type === 'image/jpeg' || 
        file.type === 'image/jpg' ||
        file.type === 'image/svg+xml';
      
      if (!isValidType) {
        message.error('Architecture diagram: Only PNG, JPG, or SVG files allowed!');
        return Upload.LIST_IGNORE;
      }

      const isLt10M = file.size / 1024 / 1024 < 10;
      if (!isLt10M) {
        message.error('Architecture diagram must be smaller than 10MB!');
        return Upload.LIST_IGNORE;
      }

      setArchitectureFile(file);
      return false;
    },
    onRemove: () => {
      setArchitectureFile(null);
    },
    fileList: architectureFile ? [architectureFile] : [],
    listType: 'picture',
  };

  // Test Cases Summary Upload Props
  const testCasesUploadProps = {
    name: 'testCasesSummary',
    maxCount: 1,
    accept: '.xlsx,.xls,.csv,.pdf',
    beforeUpload: (file) => {
      const isValidType = 
        file.name.endsWith('.xlsx') || 
        file.name.endsWith('.xls') || 
        file.name.endsWith('.csv') ||
        file.name.endsWith('.pdf');
      
      if (!isValidType) {
        message.error('Test cases: Only Excel, CSV, or PDF files allowed!');
        return Upload.LIST_IGNORE;
      }

      const isLt50M = file.size / 1024 / 1024 < 50;
      if (!isLt50M) {
        message.error('Test cases file must be smaller than 50MB!');
        return Upload.LIST_IGNORE;
      }

      setTestCasesFile(file);
      return false;
    },
    onRemove: () => {
      setTestCasesFile(null);
    },
    fileList: testCasesFile ? [testCasesFile] : [],
  };

  const handleUpload = async () => {
    if (!performanceFile) {
      message.warning('Please upload performance test data (required)');
      return;
    }

    try {
      const values = await form.validateFields();
      
      const formData = new FormData();
      formData.append('capabilityId', values.capabilityId);
      formData.append('testName', values.testName);
      formData.append('testDate', values.testDate || new Date().toISOString());
      formData.append('description', values.description || '');
      formData.append('uploadedBy', 'user'); // TODO: Get from auth context
      formData.append('performanceData', performanceFile);
      
      if (architectureFile) {
        formData.append('architectureDiagram', architectureFile);
      }
      
      if (testCasesFile) {
        formData.append('testCasesSummary', testCasesFile);
      }

      setUploading(true);
      setUploadProgress(0);

      const response = await axios.post(API_ENDPOINTS.UPLOAD_TEST_RUN, formData, {
        onUploadProgress: (progressEvent) => {
          const percentCompleted = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
          setUploadProgress(percentCompleted);
        },
      });

      message.success('Files uploaded successfully!');
      console.log('Upload response:', response.data);
      
      // Reset form
      setPerformanceFile(null);
      setArchitectureFile(null);
      setTestCasesFile(null);
      form.resetFields();
      setUploadProgress(0);
      
    } catch (error) {
      console.error('Upload error:', error);
      message.error(error.response?.data?.error || 'Upload failed. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <Space direction="vertical" size="large" style={{ width: '100%', padding: '24px' }}>
      <Title level={2}>Upload Test Results</Title>

      <Alert
        message="Enhanced Multi-File Upload"
        description="Upload performance test data along with optional architecture diagrams and test cases summary for comprehensive reporting."
        type="info"
        showIcon
        closable
      />

      <Card>
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="capabilityId"
                label="Capability"
                rules={[{ required: true, message: 'Please select a capability' }]}
              >
                <Select
                  placeholder={capabilityOptions.length > 0 ? 'Select capability' : 'No capabilities available'}
                  options={capabilityOptions}
                  size="large"
                  loading={loadingCapabilities}
                  disabled={loadingCapabilities || capabilityOptions.length === 0}
                />
              </Form.Item>
              {!loadingCapabilities && capabilityOptions.length === 0 && (
                <Text type="secondary">
                  No capabilities found. Add one via the API, then refresh this page.
                </Text>
              )}
            </Col>
            <Col span={12}>
              <Form.Item
                name="testName"
                label="Test Name"
                rules={[{ required: true, message: 'Please enter a test name' }]}
              >
                <input
                  placeholder="e.g., API Load Test - Sprint 23"
                  style={{
                    width: '100%',
                    padding: '8px 11px',
                    fontSize: '14px',
                    border: '1px solid #d9d9d9',
                    borderRadius: '6px',
                  }}
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="description"
            label="Description (Optional)"
          >
            <textarea
              placeholder="Add any additional notes about this test run"
              rows={2}
              style={{
                width: '100%',
                padding: '8px 11px',
                fontSize: '14px',
                border: '1px solid #d9d9d9',
                borderRadius: '6px',
              }}
            />
          </Form.Item>
        </Form>

        <Divider orientation="left">
          <Text strong>1. Performance Test Data (Required)</Text>
        </Divider>
        <Dragger {...performanceUploadProps}>
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">Click or drag JMeter/CSV/Excel file here</p>
          <p className="ant-upload-hint">
            Supported formats: .jtl, .csv, .xlsx, .xls (Max: 100MB)
          </p>
        </Dragger>

        <Divider orientation="left">
          <Text strong>2. Architecture Diagram (Optional)</Text>
        </Divider>
        <Upload {...architectureUploadProps}>
          <Button icon={<FileImageOutlined />}>
            Upload Architecture Diagram (PNG/JPG/SVG)
          </Button>
          <Text type="secondary" style={{ marginLeft: 8 }}>
            Max: 10MB
          </Text>
        </Upload>

        <Divider orientation="left">
          <Text strong>3. Test Cases Summary (Optional)</Text>
        </Divider>
        <Upload {...testCasesUploadProps}>
          <Button icon={<FileExcelOutlined />}>
            Upload Test Cases (Excel/CSV/PDF)
          </Button>
          <Text type="secondary" style={{ marginLeft: 8 }}>
            Max: 50MB
          </Text>
        </Upload>

        {uploading && (
          <div style={{ marginTop: 24 }}>
            <Progress percent={uploadProgress} status="active" />
            <Text type="secondary" style={{ marginTop: 8, display: 'block' }}>
              Uploading files... Please wait.
            </Text>
          </div>
        )}

        <div style={{ marginTop: 24, textAlign: 'right' }}>
          <Space>
            <Button
              onClick={() => {
                setPerformanceFile(null);
                setArchitectureFile(null);
                setTestCasesFile(null);
                form.resetFields();
              }}
              disabled={uploading}
            >
              Clear All
            </Button>
            <Button
              type="primary"
              onClick={handleUpload}
              disabled={!performanceFile}
              loading={uploading}
              icon={<UploadOutlined />}
            >
              {uploading ? 'Uploading...' : 'Upload All Files'}
            </Button>
          </Space>
        </div>
      </Card>
    </Space>
  );
};

export default EnhancedFileUpload;

// Made with Bob