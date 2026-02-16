import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from '../utils/axios';
import './ReportBuilder.css';

const ReportBuilder = () => {
  const { testRunId } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [testRun, setTestRun] = useState(null);
  const [activeSection, setActiveSection] = useState('introduction');
  const [saving, setSaving] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [includeBaseline, setIncludeBaseline] = useState(false);
  const [includePdf, setIncludePdf] = useState(false);

  const [formData, setFormData] = useState({
    // Basic Information
    capabilityName: '',
    description: '',
    
    // Narrative Sections (arrays of strings)
    introduction: [''],
    benchmarkGoals: [''],
    testSetup: [''],
    hardwareInfo: [''],
    
    // Test Scenarios (up to 10)
    scenario1: [''],
    scenario2: [''],
    scenario3: [''],
    scenario4: [''],
    scenario5: [''],
    scenario6: [''],
    scenario7: [''],
    scenario8: [''],
    scenario9: [''],
    scenario10: [''],
    
    // Analysis Sections
    performanceAnalysis: [''],
    capacityPlanning: [''],
    conclusions: [''],
    additionalNotes: [''],
    
    // Architecture
    architectureDescription: '',
    architectureDiagram: null,
    testInfrastructure: '',
    cicdWorkflow: ''
  });

  useEffect(() => {
    fetchTestRun();
  }, [testRunId]);

  const fetchTestRun = async () => {
    try {
      setLoading(true);
      const response = await axios.get(`/api/v1/test-runs/${testRunId}`);
      setTestRun(response.data);
      
      // Load existing document data if available
      if (response.data.documentData) {
        const docData = JSON.parse(response.data.documentData);
        setFormData(prevData => ({
          ...prevData,
          ...docData
        }));
      }
    } catch (error) {
      console.error('Error fetching test run:', error);
      alert('Failed to load test run data');
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleArrayChange = (field, index, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: prev[field].map((item, i) => i === index ? value : item)
    }));
  };

  const addArrayItem = (field) => {
    setFormData(prev => ({
      ...prev,
      [field]: [...prev[field], '']
    }));
  };

  const removeArrayItem = (field, index) => {
    setFormData(prev => ({
      ...prev,
      [field]: prev[field].filter((_, i) => i !== index)
    }));
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setFormData(prev => ({
        ...prev,
        architectureDiagram: file
      }));
    }
  };

  const saveDocumentData = async () => {
    try {
      setSaving(true);
      await axios.post(`/api/v1/test-runs/${testRunId}/document-data`, formData);
      alert('Document data saved successfully!');
    } catch (error) {
      console.error('Error saving document data:', error);
      alert('Failed to save document data');
    } finally {
      setSaving(false);
    }
  };

  const generateReport = async (e) => {
    // Prevent any default behavior
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    
    try {
      setGenerating(true);

      // Validate cover page required fields before generation
      try {
        const validation = await axios.get(`/api/v1/reports/validate/jmeter/${testRunId}`);
        if (!validation.data.success) {
          const missing = validation.data.missingFields || [];
          alert(`Missing required fields: ${missing.join(', ')}`);
          return;
        }
      } catch (validationError) {
        const missing = validationError.response?.data?.missingFields || [];
        if (missing.length > 0) {
          alert(`Missing required fields: ${missing.join(', ')}`);
          return;
        }
      }
      
      // First save the document data
      try {
        await axios.post(`/api/v1/test-runs/${testRunId}/document-data`, formData);
      } catch (saveError) {
        console.warn('Could not save document data:', saveError);
        // Continue with report generation even if save fails
      }
      
      // Then generate the report using testRunId
      const endpoint = includePdf
        ? `/api/v1/reports/generate/jmeter/${testRunId}/both`
        : `/api/v1/reports/generate/jmeter/${testRunId}`;
      const response = await axios.post(endpoint, null, { params: { includeBaseline } });
      
      if (response.data.success) {
        alert('Report generated successfully!');
        // Download the report
        if (response.data.data.downloadUrl) {
          window.location.href = response.data.data.downloadUrl;
        }
      } else {
        alert('Failed to generate report: ' + response.data.message);
      }
    } catch (error) {
      console.error('Error generating report:', error);
      const errorMessage = error.response?.data?.message || error.message || 'Unknown error';
      alert('Failed to generate report: ' + errorMessage);
    } finally {
      setGenerating(false);
    }
  };

  const sections = [
    { id: 'basic', label: 'Basic Information', icon: '📋' },
    { id: 'introduction', label: 'Introduction', icon: '📖' },
    { id: 'benchmarkGoals', label: 'Benchmark Goals', icon: '🎯' },
    { id: 'testSetup', label: 'Test Harness', icon: '🔧' },
    { id: 'architecture', label: 'Architecture', icon: '🏗️' },
    { id: 'hardwareInfo', label: 'Hardware/Infrastructure', icon: '💻' },
    { id: 'scenarios', label: 'Test Scenarios', icon: '📊' },
    { id: 'performanceAnalysis', label: 'Performance Analysis', icon: '📈' },
    { id: 'capacityPlanning', label: 'Capacity Planning', icon: '📐' },
    { id: 'conclusions', label: 'Conclusions', icon: '✅' },
    { id: 'additionalNotes', label: 'Appendix', icon: '📎' }
  ];

  const renderArrayInput = (field, label, placeholder) => (
    <div className="array-input-section">
      <div className="section-header">
        <h3>{label}</h3>
        <button 
          type="button" 
          onClick={() => addArrayItem(field)}
          className="btn-add-item"
        >
          + Add Item
        </button>
      </div>
      <div className="array-items">
        {formData[field].map((item, index) => (
          <div key={index} className="array-item">
            <textarea
              value={item}
              onChange={(e) => handleArrayChange(field, index, e.target.value)}
              placeholder={`${placeholder} ${index + 1}`}
              rows={3}
              className="form-textarea"
            />
            {formData[field].length > 1 && (
              <button
                type="button"
                onClick={() => removeArrayItem(field, index)}
                className="btn-remove-item"
              >
                ✕
              </button>
            )}
          </div>
        ))}
      </div>
      <div className="help-text">
        <strong>Example:</strong> Each item will appear as a separate paragraph or bullet point in the report.
      </div>
    </div>
  );

  const renderBasicInfo = () => (
    <div className="form-section">
      <h2>Basic Information</h2>
      
      <div className="form-group">
        <label>Capability Name *</label>
        <input
          type="text"
          value={formData.capabilityName}
          onChange={(e) => handleInputChange('capabilityName', e.target.value)}
          placeholder="e.g., Payments, Messaging, Onboarding"
          className="form-input"
          required
        />
      </div>

      <div className="form-group">
        <label>Description</label>
        <textarea
          value={formData.description}
          onChange={(e) => handleInputChange('description', e.target.value)}
          placeholder="Brief description of the performance test"
          rows={3}
          className="form-textarea"
        />
      </div>

      {testRun && (
        <div className="test-run-info">
          <h3>Test Run Information</h3>
          <div className="info-grid">
            <div className="info-item">
              <span className="label">Test Date:</span>
              <span className="value">{new Date(testRun.testDate).toLocaleDateString()}</span>
            </div>
            <div className="info-item">
              <span className="label">Status:</span>
              <span className={`value status-${testRun.status.toLowerCase()}`}>
                {testRun.status}
              </span>
            </div>
            <div className="info-item">
              <span className="label">Duration:</span>
              <span className="value">{testRun.duration} minutes</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );

  const renderIntroduction = () => (
    <div className="form-section">
      <h2>Introduction</h2>
      <p className="section-description">
        Describe the purpose and scope of the performance test. This section should explain 
        what was tested and why.
      </p>
      {renderArrayInput('introduction', 'Introduction Paragraphs', 'Enter introduction paragraph')}
      
      <div className="example-box">
        <h4>Example Content:</h4>
        <ul>
          <li>The idea of performance benchmarking is to achieve the maximum throughput...</li>
          <li>A load generator was used to generate the load in the form of virtual users...</li>
          <li>The load generator's state was constantly monitored.</li>
        </ul>
      </div>
    </div>
  );

  const renderTestSetup = () => (
    <div className="form-section">
      <h2>Test Harness</h2>
      <p className="section-description">
        Describe the tools, utilities, and environment used for testing. Include metrics collected.
      </p>
      {renderArrayInput('testSetup', 'Test Setup Details', 'Enter test setup detail')}
      
      <div className="example-box">
        <h4>Example Content:</h4>
        <ul>
          <li><strong>Tools used:</strong></li>
          <li>• Apache JMeter 5.3</li>
          <li>• Perfmon (JMeter Plugin)</li>
          <li><strong>Metrics collected:</strong></li>
          <li>• CPU utilization</li>
          <li>• Memory utilization</li>
          <li>• Disk utilization</li>
          <li>• Network utilization</li>
        </ul>
      </div>
    </div>
  );

  const renderArchitecture = () => (
    <div className="form-section">
      <h2>Architecture</h2>
      
      <div className="form-group">
        <label>Architecture Description</label>
        <textarea
          value={formData.architectureDescription}
          onChange={(e) => handleInputChange('architectureDescription', e.target.value)}
          placeholder="Describe the system architecture, components, and integration points"
          rows={5}
          className="form-textarea"
        />
      </div>

      <div className="form-group">
        <label>Architecture Diagram</label>
        <input
          type="file"
          accept="image/*"
          onChange={handleFileChange}
          className="form-file-input"
        />
        {formData.architectureDiagram && (
          <div className="file-preview">
            Selected: {formData.architectureDiagram.name}
          </div>
        )}
      </div>

      <div className="form-group">
        <label>Test Infrastructure</label>
        <textarea
          value={formData.testInfrastructure}
          onChange={(e) => handleInputChange('testInfrastructure', e.target.value)}
          placeholder="Describe the test infrastructure setup"
          rows={4}
          className="form-textarea"
        />
      </div>

      <div className="form-group">
        <label>CI/CD Workflow</label>
        <textarea
          value={formData.cicdWorkflow}
          onChange={(e) => handleInputChange('cicdWorkflow', e.target.value)}
          placeholder="Describe the CI/CD workflow if applicable"
          rows={4}
          className="form-textarea"
        />
      </div>
    </div>
  );

  const renderScenarios = () => (
    <div className="form-section">
      <h2>Test Scenarios</h2>
      <p className="section-description">
        Define up to 10 test scenarios. Each scenario should describe the test objective, 
        load pattern, duration, and expected outcome.
      </p>
      
      {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map(num => (
        <div key={num} className="scenario-section">
          <h3>Scenario {num}</h3>
          {renderArrayInput(
            `scenario${num}`, 
            `Scenario ${num} Details`, 
            `Enter scenario ${num} detail`
          )}
        </div>
      ))}

      <div className="example-box">
        <h4>Example Scenario:</h4>
        <ul>
          <li><strong>Scenario 1: Baseline Performance Test</strong></li>
          <li>• Objective: Establish baseline performance metrics</li>
          <li>• Virtual Users: 100 concurrent users</li>
          <li>• Duration: 30 minutes</li>
          <li>• Ramp-up: 5 minutes</li>
        </ul>
      </div>
    </div>
  );

  const renderPerformanceAnalysis = () => (
    <div className="form-section">
      <h2>Performance Analysis</h2>
      <p className="section-description">
        Provide detailed analysis of the test results, including bottleneck identification, 
        performance trends, and scalability observations.
      </p>
      {renderArrayInput('performanceAnalysis', 'Analysis Points', 'Enter analysis point')}
      
      <div className="example-box">
        <h4>Example Content:</h4>
        <ul>
          <li>The memory allocated to all products was sufficient.</li>
          <li>Disk utilization and network utilization were non-critical resources.</li>
          <li>Only CPU utilizations are being displayed in the graphs.</li>
        </ul>
      </div>
    </div>
  );

  const renderAdditionalNotes = () => (
    <div className="form-section">
      <h2>Appendix - Product Tuning</h2>
      <p className="section-description">
        Document any product tuning, configuration changes, or optimizations applied during testing.
      </p>
      {renderArrayInput('additionalNotes', 'Tuning Details', 'Enter tuning detail')}
      
      <div className="example-box">
        <h4>Example Content:</h4>
        <ul>
          <li>The following parameters were changed to improve performance:</li>
          <li>• All Integration Server logging except Error logging was disabled</li>
          <li>• Server logger was set to INFO level</li>
          <li>• Java options: wrapper.java.initmemory=8192M</li>
          <li>• Maximum threads of Server Thread pool: 500 threads</li>
        </ul>
      </div>
    </div>
  );

  const renderSection = () => {
    switch (activeSection) {
      case 'basic':
        return renderBasicInfo();
      case 'introduction':
        return renderIntroduction();
      case 'benchmarkGoals':
        return renderArrayInput('benchmarkGoals', 'Benchmark Goals', 'Enter benchmark goal');
      case 'testSetup':
        return renderTestSetup();
      case 'architecture':
        return renderArchitecture();
      case 'hardwareInfo':
        return renderArrayInput('hardwareInfo', 'Hardware/Infrastructure', 'Enter hardware detail');
      case 'scenarios':
        return renderScenarios();
      case 'performanceAnalysis':
        return renderPerformanceAnalysis();
      case 'capacityPlanning':
        return renderArrayInput('capacityPlanning', 'Capacity Planning', 'Enter capacity planning detail');
      case 'conclusions':
        return renderArrayInput('conclusions', 'Conclusions & Recommendations', 'Enter conclusion');
      case 'additionalNotes':
        return renderAdditionalNotes();
      default:
        return null;
    }
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading test run data...</p>
      </div>
    );
  }

  return (
    <div className="report-builder">
      <div className="report-builder-header">
        <h1>📝 Report Builder</h1>
        <p>Create comprehensive technical report for Test Run #{testRunId}</p>
        <div className="header-actions">
          <label className="baseline-toggle">
            <input
              type="checkbox"
              checked={includeBaseline}
              onChange={(e) => setIncludeBaseline(e.target.checked)}
            />
            Compare to baseline
          </label>
          <label className="baseline-toggle">
            <input
              type="checkbox"
              checked={includePdf}
              onChange={(e) => setIncludePdf(e.target.checked)}
            />
            Also generate PDF
          </label>
          <button 
            onClick={saveDocumentData} 
            disabled={saving}
            className="btn btn-secondary"
          >
            {saving ? 'Saving...' : '💾 Save Draft'}
          </button>
          <button
            onClick={(e) => generateReport(e)}
            disabled={generating}
            className="btn btn-primary"
            type="button"
          >
            {generating
              ? 'Generating...'
              : includePdf
                ? '📄 Generate Word + PDF'
                : '📄 Generate Word'}
          </button>
        </div>
      </div>

      <div className="report-builder-content">
        <div className="sidebar">
          <h3>Report Sections</h3>
          <nav className="section-nav">
            {sections.map(section => (
              <button
                key={section.id}
                onClick={() => setActiveSection(section.id)}
                className={`nav-item ${activeSection === section.id ? 'active' : ''}`}
              >
                <span className="icon">{section.icon}</span>
                <span className="label">{section.label}</span>
              </button>
            ))}
          </nav>
        </div>

        <div className="main-content">
          {renderSection()}
        </div>
      </div>
    </div>
  );
};

export default ReportBuilder;

// Made with Bob
