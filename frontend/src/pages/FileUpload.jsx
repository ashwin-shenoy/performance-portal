import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Grid,
  Column,
  Tile,
  Button,
  FileUploader,
  Form,
  TextInput,
  Select,
  SelectItem,
  TextArea,
  ProgressBar,
  InlineNotification,
  Loading,
} from '@carbon/react';
import {
  Upload,
  CloudUpload,
  CheckmarkFilled,
} from '@carbon/icons-react';
import axios from '../utils/axios';
import { API_ENDPOINTS } from '../config/api';

const FileUpload = () => {
  const navigate = useNavigate();
  const [files, setFiles] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [generatingReport, setGeneratingReport] = useState(false);
  const [latestTestRunId, setLatestTestRunId] = useState(null);
  const [testRunStatus, setTestRunStatus] = useState(null);
  const [pollingStatus, setPollingStatus] = useState(false);
  const [capabilities, setCapabilities] = useState([]);
  const [loadingCapabilities, setLoadingCapabilities] = useState(true);
  const [notification, setNotification] = useState(null);
  const hasCapabilities = capabilities.length > 0;
  
  // Form state
  const [formData, setFormData] = useState({
    capability: '',
    testName: '',
    buildNumber: '',
    description: '',
  });

  useEffect(() => {
    fetchCapabilities();
  }, []);

  useEffect(() => {
    if (!latestTestRunId) {
      return;
    }

    let isMounted = true;
    let pollTimer = null;

    const pollStatus = async () => {
      setPollingStatus(true);
      try {
        const response = await axios.get(API_ENDPOINTS.TEST_BY_ID(latestTestRunId));
        const status = response.data?.status || 'UNKNOWN';
        if (isMounted) {
          setTestRunStatus(status);
          if (status === 'COMPLETED' || status === 'FAILED') {
            setPollingStatus(false);
            if (pollTimer) {
              clearInterval(pollTimer);
            }
          }
        }
      } catch (error) {
        console.error('Error polling test run status:', error);
        if (isMounted) {
          setPollingStatus(false);
        }
      }
    };

    pollStatus();
    pollTimer = setInterval(pollStatus, 5000);

    return () => {
      isMounted = false;
      if (pollTimer) {
        clearInterval(pollTimer);
      }
    };
  }, [latestTestRunId]);

  const fetchCapabilities = async () => {
    try {
      setLoadingCapabilities(true);
      const response = await axios.get(API_ENDPOINTS.CAPABILITIES_DROPDOWN);
      setCapabilities(response.data);
    } catch (error) {
      console.error('Error fetching capabilities:', error);
      showNotification('error', 'Failed to load capabilities', 'Please refresh the page.');
      setCapabilities([]);
    } finally {
      setLoadingCapabilities(false);
    }
  };

  const showNotification = (kind, title, subtitle) => {
    setNotification({ kind, title, subtitle });
    setTimeout(() => setNotification(null), 5000);
  };

  const validateFile = (file) => {
    const isJtl = file.name.toLowerCase().endsWith('.jtl');
    
    if (!isJtl) {
      showNotification('error', 'Invalid file type', 'Only JTL files are supported');
      return false;
    }

    const isLt200M = file.size / 1024 / 1024 < 200;
    if (!isLt200M) {
      showNotification('error', 'File too large', 'File must be smaller than 200MB');
      return false;
    }

    return true;
  };

  const handleFileChange = (event) => {
    const newFiles = Array.from(event.target.files || []);
    const validFiles = newFiles.filter(validateFile);
    setFiles([...files, ...validFiles]);
  };

  const handleFileDelete = (index) => {
    const newFiles = [...files];
    newFiles.splice(index, 1);
    setFiles(newFiles);
  };

  const validateForm = () => {
    if (!formData.capability) {
      showNotification('warning', 'Missing capability', 'Please select a capability');
      return false;
    }
    if (!formData.testName) {
      showNotification('warning', 'Missing test name', 'Please enter a test name');
      return false;
    }
    if (!formData.buildNumber) {
      showNotification('warning', 'Missing build number', 'Please enter a build number');
      return false;
    }
    if (files.length === 0) {
      showNotification('warning', 'No files selected', 'Please select a JTL file to upload');
      return false;
    }
    return true;
  };

  const generateReport = async (testRunId) => {
    try {
      console.log('Generating report for test run:', testRunId);
      const response = await axios.post(API_ENDPOINTS.GENERATE_WORD_REPORT(testRunId));
      console.log('Report generated:', response.data);
      return response.data;
    } catch (error) {
      console.error('Error generating report:', error);
      throw error;
    }
  };

  const handleGenerateReport = async () => {
    if (!latestTestRunId) {
      showNotification('warning', 'No upload found', 'Upload a JTL file first.');
      return;
    }

    if (testRunStatus !== 'COMPLETED') {
      showNotification('warning', 'Processing not complete', 'Wait until the test run is completed.');
      return;
    }

    setGeneratingReport(true);
    try {
      await generateReport(latestTestRunId);
      showNotification('success', 'Report generated', 'Report has been generated successfully!');
      setTimeout(() => {
        navigate('/reports');
      }, 2000);
    } catch (reportError) {
      console.error('Report generation error:', reportError);
      showNotification('error', 'Report generation failed',
        reportError.response?.data?.message || 'Please try again from the Reports page.');
    } finally {
      setGeneratingReport(false);
    }
  };

  const handleUpload = async () => {
    if (!validateForm()) return;

    const formDataToSend = new FormData();
    files.forEach((file) => {
      formDataToSend.append('files', file);
    });
    formDataToSend.append('capability', formData.capability);
    formDataToSend.append('testName', formData.testName);
    formDataToSend.append('buildNumber', formData.buildNumber);
    formDataToSend.append('description', formData.description || '');

    setUploading(true);
    setUploadProgress(0);

    try {
      const uploadResponse = await axios.post(API_ENDPOINTS.UPLOAD, formDataToSend, {
        onUploadProgress: (progressEvent) => {
          const percentCompleted = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
          setUploadProgress(percentCompleted);
        },
      });

      console.log('Upload response:', uploadResponse.data);
      
      // Get test run ID from response
      const testRunId = uploadResponse.data.testRunId;
      if (!testRunId) {
        throw new Error('No test run ID returned from upload');
      }

      setLatestTestRunId(testRunId);
      setTestRunStatus('PROCESSING');
      showNotification(
        'success',
        'Upload successful',
        'JTL file uploaded. Wait for processing, then generate the report.'
      );
      
      // Reset form
      setFiles([]);
      setFormData({
        capability: '',
        testName: '',
        buildNumber: '',
        description: '',
      });
      setUploadProgress(0);
    } catch (error) {
      console.error('Upload error:', error);
      
      let errorMessage = 'Upload failed. Please try again.';
      if (error.code === 'ECONNABORTED') {
        errorMessage = 'Upload timeout. The file may be too large or your connection is slow.';
      } else if (error.response) {
        errorMessage = error.response.data?.message || `Server error: ${error.response.status}`;
      } else if (error.request) {
        errorMessage = 'No response from server. Please check your connection.';
      }
      
      showNotification('error', 'Upload failed', errorMessage);
    } finally {
      setUploading(false);
    }
  };

  const handleClear = () => {
    setFiles([]);
    setFormData({
      capability: '',
      testName: '',
      buildNumber: '',
      description: '',
    });
  };

  return (
    <div className="page-container fade-in">
      {/* Header */}
      <Tile style={{ padding: '2rem', marginBottom: '2rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <CloudUpload size={40} />
          <div>
            <h1 style={{ margin: 0, fontSize: '2rem', fontWeight: 400 }}>
              Upload Test Results
            </h1>
            <p style={{ margin: '0.5rem 0 0 0', fontSize: '0.875rem', color: '#525252' }}>
              Upload JMeter JTL files to generate performance reports
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

      {/* Upload Form */}
      <Tile style={{ padding: '2rem' }}>
        <Form>
          <Grid narrow>
            <Column sm={4} md={8} lg={8}>
              <Select
                id="capability-select"
                labelText="Capability *"
                value={formData.capability}
                onChange={(e) => setFormData({ ...formData, capability: e.target.value })}
                disabled={loadingCapabilities || !hasCapabilities}
                invalid={!formData.capability && files.length > 0}
                invalidText="Please select a capability"
              >
                <SelectItem
                  value=""
                  text={hasCapabilities ? 'Select capability' : 'No capabilities available'}
                />
                {capabilities.map((cap) => (
                  <SelectItem key={cap.value} value={cap.value} text={cap.label} />
                ))}
              </Select>
              {!loadingCapabilities && !hasCapabilities && (
                <p style={{ marginTop: '0.5rem', fontSize: '0.875rem', color: '#525252' }}>
                  No capabilities found. Add one via the API, then refresh this page.
                </p>
              )}
            </Column>

            <Column sm={4} md={8} lg={8}>
              <TextInput
                id="test-name"
                labelText="Test Name *"
                placeholder="e.g., API Gateway Load Test - Sprint 23"
                value={formData.testName}
                onChange={(e) => setFormData({ ...formData, testName: e.target.value })}
                invalid={!formData.testName && files.length > 0}
                invalidText="Please enter a test name"
              />
            </Column>

            <Column sm={4} md={8} lg={8}>
              <TextInput
                id="build-number"
                labelText="Build Number *"
                placeholder="e.g., 10.15.0.123"
                value={formData.buildNumber}
                onChange={(e) => setFormData({ ...formData, buildNumber: e.target.value })}
                invalid={!formData.buildNumber && files.length > 0}
                invalidText="Please enter a build number"
              />
            </Column>

            <Column sm={4} md={8} lg={16}>
              <TextArea
                id="description"
                labelText="Description (Optional)"
                placeholder="Add any additional notes about this test run"
                rows={3}
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </Column>

            <Column sm={4} md={8} lg={16}>
              <div style={{ marginTop: '1rem' }}>
                <FileUploader
                  labelTitle="Upload JTL File"
                  labelDescription="Maximum file size: 200MB. JMeter JTL files only."
                  buttonLabel="Add file"
                  buttonKind="primary"
                  size="lg"
                  filenameStatus="edit"
                  accept={['.jtl']}
                  multiple={false}
                  disabled={uploading || files.length > 0}
                  onChange={handleFileChange}
                />
                
                {files.length > 0 && (
                  <div style={{ marginTop: '1rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.75rem', background: '#f4f4f4', borderRadius: '4px' }}>
                      <CheckmarkFilled size={20} style={{ color: '#24a148' }} />
                      <span style={{ fontWeight: 500 }}>{files[0].name}</span>
                      <span style={{ color: '#525252', fontSize: '0.875rem' }}>
                        ({(files[0].size / 1024 / 1024).toFixed(2)} MB)
                      </span>
                      <Button
                        kind="ghost"
                        size="sm"
                        onClick={() => handleFileDelete(0)}
                        disabled={uploading}
                        style={{ marginLeft: 'auto' }}
                      >
                        Remove
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            </Column>

            {uploading && (
              <Column sm={4} md={8} lg={16}>
                <Tile style={{ marginTop: '1rem', padding: '1.5rem', background: '#f4f4f4' }}>
                  <ProgressBar
                    label="Uploading and processing file"
                    value={uploadProgress}
                    max={100}
                    status="active"
                  />
                  <div style={{ marginTop: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Loading small withOverlay={false} />
                    <span>Processing test data on the server. Please wait.</span>
                  </div>
                </Tile>
              </Column>
            )}

            <Column sm={4} md={8} lg={16}>
              <div style={{ marginTop: '1.5rem', display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
                <Button
                  kind="secondary"
                  onClick={handleClear}
                  disabled={uploading || files.length === 0}
                >
                  Clear
                </Button>
                <Button
                  kind="primary"
                  renderIcon={Upload}
                  onClick={handleUpload}
                  disabled={files.length === 0 || uploading}
                >
                  {uploading ? 'Uploading...' : 'Upload'}
                </Button>
              </div>
            </Column>

            <Column sm={4} md={8} lg={16}>
              <div style={{ marginTop: '1rem', display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  kind="tertiary"
                  onClick={handleGenerateReport}
                  disabled={!latestTestRunId || testRunStatus !== 'COMPLETED' || uploading || generatingReport}
                >
                  {generatingReport ? 'Generating...' : 'Generate Report'}
                </Button>
              </div>
              {latestTestRunId && (
                <p style={{ marginTop: '0.75rem', textAlign: 'right', color: '#525252' }}>
                  {pollingStatus ? 'Processing test run...' : `Status: ${testRunStatus || 'Unknown'}`}
                </p>
              )}
            </Column>
          </Grid>
        </Form>
      </Tile>

      {/* Info Section */}
      <Tile style={{ padding: '1.5rem', marginTop: '2rem', background: '#f4f4f4' }}>
        <h3 style={{ margin: '0 0 1rem 0', fontSize: '1rem', fontWeight: 500 }}>How it works</h3>
        <ol style={{ margin: 0, paddingLeft: '1.5rem', color: '#525252', lineHeight: '1.8' }}>
          <li>Upload a JMeter JTL file containing test results</li>
          <li>The system parses the test data and extracts metrics</li>
          <li>Generate the report once processing completes</li>
          <li>Access your report from the Reports page</li>
        </ol>
      </Tile>
    </div>
  );
};

export default FileUpload;

// Made with Bob
