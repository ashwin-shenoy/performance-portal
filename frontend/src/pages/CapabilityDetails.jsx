import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Tile,
  Grid,
  Column,
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
  Loading,
  InlineNotification,
  Modal,
  TextInput,
  TextArea,
  FileUploader,
} from '@carbon/react';
import { Edit, Renew, DocumentBlank, Layers, Add, CheckmarkFilled, Upload, View, Download, TrashCan } from '@carbon/icons-react';
import dayjs from 'dayjs';
import axios from '../utils/axios';
import { API_ENDPOINTS } from '../config/api';
import './CapabilityDetails.css';

const CapabilityDetails = () => {
  const navigate = useNavigate();
  const [testRuns, setTestRuns] = useState([]);
  const [filteredRuns, setFilteredRuns] = useState([]);
  const [capabilities, setCapabilities] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingCapabilities, setLoadingCapabilities] = useState(false);
  const [notification, setNotification] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterCapability, setFilterCapability] = useState('all');
  const [filterTestType, setFilterTestType] = useState('all');
  const [filterStatus, setFilterStatus] = useState('all');
  const [createOpen, setCreateOpen] = useState(false);
  const [createSaving, setCreateSaving] = useState(false);
  const [createTouched, setCreateTouched] = useState(false);
  const [createForm, setCreateForm] = useState({
    name: '',
    description: '',
    isActive: true,
  });
  const [selectedCapabilityId, setSelectedCapabilityId] = useState('');
  const [testCases, setTestCases] = useState([]);
  const [loadingTestCases, setLoadingTestCases] = useState(false);
  const [testCaseCounts, setTestCaseCounts] = useState({});
  const [loadingTestCaseCounts, setLoadingTestCaseCounts] = useState(false);
  const [testCaseModalOpen, setTestCaseModalOpen] = useState(false);
  const [testCaseSaving, setTestCaseSaving] = useState(false);
  const [testCaseTouched, setTestCaseTouched] = useState(false);
  const [editingTestCase, setEditingTestCase] = useState(null);
  const [testCaseForm, setTestCaseForm] = useState({
    testCaseName: '',
    description: '',
    expectedBehavior: '',
    priority: 'MEDIUM',
  });
  const [metadataForm, setMetadataForm] = useState({
    testObjective: '',
    testScope: '',
    environmentDetails: '',
  });
  const [metadataSaving, setMetadataSaving] = useState(false);
  const [baselineForm, setBaselineForm] = useState({
    p95MaxMs: '',
    avgMaxMs: '',
    p90MaxMs: '',
    throughputMin: '',
  });
  const [baselineSaving, setBaselineSaving] = useState(false);
  const [architectureFile, setArchitectureFile] = useState(null);
  const [architectureSaving, setArchitectureSaving] = useState(false);
  const [architectureRemoving, setArchitectureRemoving] = useState(false);
  const [documentRunId, setDocumentRunId] = useState('');
  const [documentLoading, setDocumentLoading] = useState(false);
  const [documentSaving, setDocumentSaving] = useState(false);
  const [documentForm, setDocumentForm] = useState({
    capabilityName: '',
    description: '',
    introduction: '',
    benchmarkGoals: '',
    testSetup: '',
    hardwareInfo: '',
    scenarioFields: [''],
    performanceAnalysis: '',
    capacityPlanning: '',
    conclusions: '',
    additionalNotes: '',
  });

  useEffect(() => {
    fetchTestRuns();
    fetchCapabilities();
  }, []);

  useEffect(() => {
    if (!selectedCapabilityId && capabilities.length > 0) {
      setSelectedCapabilityId(String(capabilities[0].id));
    }
  }, [capabilities, selectedCapabilityId]);

  useEffect(() => {
    if (!selectedCapabilityId) {
      setMetadataForm({ testObjective: '', testScope: '', environmentDetails: '' });
      setBaselineForm({ p95MaxMs: '', avgMaxMs: '', p90MaxMs: '', throughputMin: '' });
      setArchitectureFile(null);
      return;
    }

    const selected = capabilities.find(
      (capability) => String(capability.id) === String(selectedCapabilityId)
    );
    setMetadataForm({
      testObjective: selected?.testObjective || '',
      testScope: selected?.testScope || '',
      environmentDetails: selected?.environmentDetails || '',
    });
    setArchitectureFile(null);

    fetchBaseline(selectedCapabilityId);
  }, [capabilities, selectedCapabilityId]);

  useEffect(() => {
    if (capabilities.length > 0) {
      fetchTestCaseCounts();
    } else {
      setTestCaseCounts({});
    }
  }, [capabilities]);

  useEffect(() => {
    if (selectedCapabilityId) {
      fetchTestCases(selectedCapabilityId);
    } else {
      setTestCases([]);
    }
  }, [selectedCapabilityId]);

  useEffect(() => {
    if (!documentRunId) {
      resetDocumentForm();
      return;
    }

    fetchDocumentData(documentRunId);
  }, [documentRunId]);

  const refreshAll = () => {
    fetchTestRuns();
    fetchCapabilities();
    fetchTestCaseCounts();
  };

  useEffect(() => {
    applyFilters();
  }, [testRuns, searchTerm, filterCapability, filterTestType, filterStatus]);

  const showNotification = (kind, title, subtitle) => {
    setNotification({ kind, title, subtitle });
    setTimeout(() => setNotification(null), 5000);
  };

  const fetchTestRuns = async () => {
    setLoading(true);
    try {
      const response = await axios.get(API_ENDPOINTS.TESTS);
      setTestRuns(response.data || []);
    } catch (error) {
      console.error('Error fetching test runs:', error);
      showNotification(
        'error',
        'Failed to load test runs',
        error.response?.data?.message || error.message
      );
      setTestRuns([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchCapabilities = async () => {
    setLoadingCapabilities(true);
    try {
      const response = await axios.get(API_ENDPOINTS.CAPABILITIES);
      setCapabilities(response.data || []);
    } catch (error) {
      console.error('Error fetching capabilities:', error);
      showNotification(
        'error',
        'Failed to load capabilities',
        error.response?.data?.message || error.message
      );
      setCapabilities([]);
    } finally {
      setLoadingCapabilities(false);
    }
  };

  const fetchTestCases = async (capabilityId) => {
    setLoadingTestCases(true);
    try {
      const response = await axios.get(
        `${API_ENDPOINTS.CAPABILITIES}/${capabilityId}/test-cases`
      );
      setTestCases(response.data || []);
    } catch (error) {
      console.error('Error fetching test cases:', error);
      showNotification(
        'error',
        'Failed to load test cases',
        error.response?.data?.message || error.message
      );
      setTestCases([]);
    } finally {
      setLoadingTestCases(false);
    }
  };

  const fetchTestCaseCounts = async () => {
    setLoadingTestCaseCounts(true);
    try {
      const response = await axios.get(API_ENDPOINTS.CAPABILITY_TEST_CASE_COUNTS);
      const rawCounts = response.data || {};
      const normalized = Object.fromEntries(
        Object.entries(rawCounts).map(([key, value]) => [Number(key), Number(value)])
      );
      setTestCaseCounts(normalized);
    } catch (error) {
      console.error('Error fetching test case counts:', error);
      showNotification(
        'error',
        'Failed to load test case counts',
        error.response?.data?.message || error.message
      );
      setTestCaseCounts({});
    } finally {
      setLoadingTestCaseCounts(false);
    }
  };

  const fetchBaseline = async (capabilityId) => {
    try {
      const response = await axios.get(API_ENDPOINTS.CAPABILITY_BASELINE(capabilityId));
      const baseline = response.data?.baseline || response.data || {};
      setBaselineForm({
        p95MaxMs: baseline.p95MaxMs?.toString() || '',
        avgMaxMs: baseline.avgMaxMs?.toString() || '',
        p90MaxMs: baseline.p90MaxMs?.toString() || '',
        throughputMin: baseline.throughputMin?.toString() || '',
      });
    } catch (error) {
      console.error('Error fetching baseline:', error);
      setBaselineForm({ p95MaxMs: '', avgMaxMs: '', p90MaxMs: '', throughputMin: '' });
    }
  };

  const applyFilters = () => {
    let filtered = [...testRuns];

    if (filterCapability !== 'all') {
      filtered = filtered.filter((test) => test.capability === filterCapability);
    }

    if (filterTestType !== 'all') {
      filtered = filtered.filter((test) => test.testType === filterTestType);
    }

    if (filterStatus !== 'all') {
      filtered = filtered.filter((test) => test.status === filterStatus);
    }

    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter((test) =>
        test.testName?.toLowerCase().includes(term) ||
        test.capability?.toLowerCase().includes(term) ||
        test.buildNumber?.toLowerCase().includes(term)
      );
    }

    setFilteredRuns(filtered);
  };

  const capabilityOptions = useMemo(() => {
    const names = capabilities
      .map((capability) => capability.name)
      .filter(Boolean);
    return ['all', ...new Set(names)];
  }, [capabilities]);

  const testTypeOptions = useMemo(
    () => ['all', ...new Set(testRuns.map((test) => test.testType).filter(Boolean))],
    [testRuns]
  );

  const statusOptions = useMemo(
    () => ['all', ...new Set(testRuns.map((test) => test.status).filter(Boolean))],
    [testRuns]
  );

  const selectedCapability = useMemo(() => {
    if (!selectedCapabilityId) {
      return null;
    }
    return capabilities.find(
      (capability) => String(capability.id) === String(selectedCapabilityId)
    ) || null;
  }, [capabilities, selectedCapabilityId]);

  const architecturePreviewUrl = selectedCapabilityId
    ? API_ENDPOINTS.CAPABILITY_ARCHITECTURE_PREVIEW(selectedCapabilityId)
    : '';
  const architectureDownloadUrl = selectedCapabilityId
    ? API_ENDPOINTS.CAPABILITY_ARCHITECTURE_DOWNLOAD(selectedCapabilityId)
    : '';

  const handleCreateChange = (field, value) => {
    setCreateForm((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const resetCreateForm = () => {
    setCreateForm({ name: '', description: '', isActive: true });
    setCreateTouched(false);
  };

  const resetTestCaseForm = () => {
    setTestCaseForm({
      testCaseName: '',
      description: '',
      expectedBehavior: '',
      priority: 'MEDIUM',
    });
    setEditingTestCase(null);
    setTestCaseTouched(false);
  };

  const resetDocumentForm = () => {
    setDocumentForm({
      capabilityName: '',
      description: '',
      introduction: '',
      benchmarkGoals: '',
      testSetup: '',
      hardwareInfo: '',
      scenarioFields: [''],
      performanceAnalysis: '',
      capacityPlanning: '',
      conclusions: '',
      additionalNotes: '',
    });
  };

  const formatLines = (value) => {
    if (!Array.isArray(value)) {
      return '';
    }
    return value.join('\n');
  };

  const parseLines = (value) => {
    if (value === null || value === undefined) {
      return [];
    }
    return String(value)
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean);
  };

  const normalizeScenarioFields = (docData) => {
    const scenarios = [];
    for (let i = 1; i <= 10; i += 1) {
      const field = docData?.[`scenario${i}`];
      const formatted = formatLines(field);
      if (formatted) {
        scenarios.push(formatted);
      }
    }
    return scenarios.length > 0 ? scenarios : [''];
  };

  const openCreateTestCase = () => {
    resetTestCaseForm();
    setTestCaseModalOpen(true);
  };

  const openEditTestCase = (testCase) => {
    setEditingTestCase(testCase);
    setTestCaseForm({
      testCaseName: testCase.testCaseName || '',
      description: testCase.description || '',
      expectedBehavior: testCase.expectedBehavior || '',
      priority: testCase.priority || 'MEDIUM',
    });
    setTestCaseTouched(false);
    setTestCaseModalOpen(true);
  };

  const handleTestCaseChange = (field, value) => {
    setTestCaseForm((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleMetadataChange = (field, value) => {
    setMetadataForm((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleBaselineChange = (field, value) => {
    setBaselineForm((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleDocumentChange = (field, value) => {
    setDocumentForm((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleScenarioChange = (index, value) => {
    setDocumentForm((prev) => ({
      ...prev,
      scenarioFields: prev.scenarioFields.map((item, i) => (i === index ? value : item)),
    }));
  };

  const addScenarioField = () => {
    setDocumentForm((prev) => {
      if (prev.scenarioFields.length >= 10) {
        return prev;
      }
      return { ...prev, scenarioFields: [...prev.scenarioFields, ''] };
    });
  };

  const removeScenarioField = (index) => {
    setDocumentForm((prev) => {
      const next = prev.scenarioFields.filter((_, i) => i !== index);
      return { ...prev, scenarioFields: next.length ? next : [''] };
    });
  };

  const parseBaselineNumber = (value) => {
    if (value === null || value === undefined) {
      return null;
    }
    const normalized = String(value).trim();
    if (!normalized) {
      return null;
    }
    const numeric = Number(normalized);
    return Number.isFinite(numeric) ? numeric : null;
  };

  const fetchDocumentData = async (testRunId) => {
    setDocumentLoading(true);
    try {
      const response = await axios.get(API_ENDPOINTS.TEST_BY_ID(testRunId));
      const testRun = response.data || {};
      let docData = {};

      if (testRun.documentData) {
        try {
          docData = JSON.parse(testRun.documentData);
        } catch (parseError) {
          console.warn('Unable to parse document data JSON:', parseError);
        }
      }

      setDocumentForm({
        capabilityName: docData.capabilityName || testRun.capability || '',
        description: docData.description || '',
        introduction: formatLines(docData.introduction),
        benchmarkGoals: formatLines(docData.benchmarkGoals),
        testSetup: formatLines(docData.testSetup),
        hardwareInfo: formatLines(docData.hardwareInfo),
        scenarioFields: normalizeScenarioFields(docData),
        performanceAnalysis: formatLines(docData.performanceAnalysis),
        capacityPlanning: formatLines(docData.capacityPlanning),
        conclusions: formatLines(docData.conclusions),
        additionalNotes: formatLines(docData.additionalNotes),
      });
    } catch (error) {
      console.error('Error fetching document data:', error);
      showNotification(
        'error',
        'Failed to load narrative data',
        error.response?.data?.message || error.message
      );
      resetDocumentForm();
    } finally {
      setDocumentLoading(false);
    }
  };

  const handleSaveBaseline = async () => {
    if (!selectedCapabilityId) {
      showNotification('error', 'No capability selected', 'Select a capability first');
      return;
    }

    setBaselineSaving(true);
    try {
      const payload = {
        p95MaxMs: parseBaselineNumber(baselineForm.p95MaxMs),
        avgMaxMs: parseBaselineNumber(baselineForm.avgMaxMs),
        p90MaxMs: parseBaselineNumber(baselineForm.p90MaxMs),
        throughputMin: parseBaselineNumber(baselineForm.throughputMin),
      };

      const response = await axios.put(
        API_ENDPOINTS.CAPABILITY_BASELINE(selectedCapabilityId),
        payload
      );
      const updated = response.data?.baseline || response.data || payload;
      setBaselineForm({
        p95MaxMs: updated.p95MaxMs?.toString() || '',
        avgMaxMs: updated.avgMaxMs?.toString() || '',
        p90MaxMs: updated.p90MaxMs?.toString() || '',
        throughputMin: updated.throughputMin?.toString() || '',
      });
      showNotification('success', 'Baseline saved', 'Baseline metrics updated');
      fetchCapabilities();
    } catch (error) {
      console.error('Error saving baseline:', error);
      showNotification(
        'error',
        'Save failed',
        error.response?.data?.message || error.message
      );
    } finally {
      setBaselineSaving(false);
    }
  };

  const handleSaveDocumentData = async () => {
    if (!documentRunId) {
      showNotification('error', 'No test run selected', 'Select a test run first');
      return;
    }

    setDocumentSaving(true);
    try {
      const payload = {
        capabilityName: documentForm.capabilityName.trim(),
        description: documentForm.description.trim(),
        introduction: parseLines(documentForm.introduction),
        benchmarkGoals: parseLines(documentForm.benchmarkGoals),
        testSetup: parseLines(documentForm.testSetup),
        hardwareInfo: parseLines(documentForm.hardwareInfo),
        performanceAnalysis: parseLines(documentForm.performanceAnalysis),
        capacityPlanning: parseLines(documentForm.capacityPlanning),
        conclusions: parseLines(documentForm.conclusions),
        additionalNotes: parseLines(documentForm.additionalNotes),
      };

      for (let i = 1; i <= 10; i += 1) {
        const value = documentForm.scenarioFields[i - 1] || '';
        payload[`scenario${i}`] = parseLines(value);
      }

      await axios.post(API_ENDPOINTS.DOCUMENT_DATA(documentRunId), payload);
      showNotification('success', 'Narrative data saved', 'Document data saved for the test run.');
    } catch (error) {
      console.error('Error saving document data:', error);
      showNotification(
        'error',
        'Failed to save narrative data',
        error.response?.data?.message || error.message
      );
    } finally {
      setDocumentSaving(false);
    }
  };

  const handleCreateCapability = async () => {
    setCreateTouched(true);
    if (!createForm.name.trim()) {
      return;
    }

    setCreateSaving(true);
    try {
      await axios.post(API_ENDPOINTS.CAPABILITIES, {
        name: createForm.name.trim(),
        description: createForm.description.trim(),
        isActive: createForm.isActive,
      });
      showNotification('success', 'Capability created', 'Capability added successfully');
      setCreateOpen(false);
      resetCreateForm();
      fetchCapabilities();
      fetchTestCaseCounts();
    } catch (error) {
      console.error('Error creating capability:', error);
      showNotification(
        'error',
        'Create capability failed',
        error.response?.data?.message || error.message
      );
    } finally {
      setCreateSaving(false);
    }
  };

  const handleSaveTestCase = async () => {
    setTestCaseTouched(true);
    if (!testCaseForm.testCaseName.trim() || !selectedCapabilityId) {
      return;
    }

    setTestCaseSaving(true);
    try {
      const payload = {
        testCaseName: testCaseForm.testCaseName.trim(),
        description: testCaseForm.description.trim(),
        expectedBehavior: testCaseForm.expectedBehavior.trim(),
        priority: testCaseForm.priority,
      };

      if (editingTestCase) {
        await axios.put(
          `${API_ENDPOINTS.CAPABILITIES}/${selectedCapabilityId}/test-cases/${editingTestCase.id}`,
          payload
        );
        showNotification('success', 'Test case updated', 'Test case saved successfully');
      } else {
        await axios.post(
          `${API_ENDPOINTS.CAPABILITIES}/${selectedCapabilityId}/test-cases`,
          payload
        );
        showNotification('success', 'Test case created', 'Test case added successfully');
      }

      setTestCaseModalOpen(false);
      resetTestCaseForm();
      fetchTestCases(selectedCapabilityId);
      fetchTestCaseCounts();
    } catch (error) {
      console.error('Error saving test case:', error);
      showNotification(
        'error',
        'Save test case failed',
        error.response?.data?.message || error.message
      );
    } finally {
      setTestCaseSaving(false);
    }
  };

  const handleDeleteTestCase = async (testCaseId) => {
    if (!selectedCapabilityId) {
      return;
    }

    const confirmed = window.confirm('Delete this test case? This action cannot be undone.');
    if (!confirmed) {
      return;
    }

    try {
      await axios.delete(
        `${API_ENDPOINTS.CAPABILITIES}/${selectedCapabilityId}/test-cases/${testCaseId}`
      );
      showNotification('success', 'Test case deleted', 'Test case removed successfully');
      fetchTestCases(selectedCapabilityId);
      fetchTestCaseCounts();
    } catch (error) {
      console.error('Error deleting test case:', error);
      showNotification(
        'error',
        'Delete test case failed',
        error.response?.data?.message || error.message
      );
    }
  };

  const handleSaveMetadata = async () => {
    if (!selectedCapabilityId) {
      showNotification('error', 'No capability selected', 'Select a capability first');
      return;
    }

    setMetadataSaving(true);
    try {
      await axios.put(
        `${API_ENDPOINTS.CAPABILITIES}/${selectedCapabilityId}/metadata`,
        {
          testObjective: metadataForm.testObjective.trim(),
          testScope: metadataForm.testScope.trim(),
          environmentDetails: metadataForm.environmentDetails.trim(),
        }
      );
      showNotification('success', 'Metadata saved', 'Capability details updated');
      fetchCapabilities();
    } catch (error) {
      console.error('Error saving metadata:', error);
      showNotification(
        'error',
        'Save failed',
        error.response?.data?.message || error.message
      );
    } finally {
      setMetadataSaving(false);
    }
  };

  const validateArchitectureFile = (file) => {
    const allowedTypes = ['image/png', 'image/jpeg', 'image/jpg', 'image/svg+xml'];
    if (!allowedTypes.includes(file.type)) {
      showNotification('error', 'Invalid file type', 'Use PNG, JPG, or SVG');
      return false;
    }

    const isLt10M = file.size / 1024 / 1024 < 10;
    if (!isLt10M) {
      showNotification('error', 'File too large', 'Architecture diagrams must be under 10MB');
      return false;
    }

    return true;
  };

  const handleArchitectureFileChange = (event) => {
    const newFiles = Array.from(event.target.files || []);
    if (newFiles.length === 0) {
      return;
    }
    const nextFile = newFiles[0];
    if (validateArchitectureFile(nextFile)) {
      setArchitectureFile(nextFile);
    }
  };

  const clearArchitectureFile = () => {
    setArchitectureFile(null);
  };

  const handleUploadArchitectureDiagram = async () => {
    if (!selectedCapabilityId) {
      showNotification('error', 'No capability selected', 'Select a capability first');
      return;
    }
    if (!architectureFile) {
      showNotification('error', 'No file selected', 'Choose an architecture diagram to upload');
      return;
    }

    setArchitectureSaving(true);
    try {
      const formData = new FormData();
      formData.append('file', architectureFile);
      await axios.post(
        API_ENDPOINTS.CAPABILITY_ARCHITECTURE_DIAGRAM(selectedCapabilityId),
        formData
      );
      showNotification('success', 'Diagram uploaded', 'Architecture diagram saved');
      clearArchitectureFile();
      fetchCapabilities();
    } catch (error) {
      console.error('Error uploading architecture diagram:', error);
      showNotification(
        'error',
        'Upload failed',
        error.response?.data?.message || error.message
      );
    } finally {
      setArchitectureSaving(false);
    }
  };

  const handleRemoveArchitectureDiagram = async () => {
    if (!selectedCapabilityId) {
      showNotification('error', 'No capability selected', 'Select a capability first');
      return;
    }

    const confirmed = window.confirm('Remove the current architecture diagram?');
    if (!confirmed) {
      return;
    }

    setArchitectureRemoving(true);
    try {
      await axios.delete(API_ENDPOINTS.CAPABILITY_ARCHITECTURE_DELETE(selectedCapabilityId));
      showNotification('success', 'Diagram removed', 'Architecture diagram deleted');
      clearArchitectureFile();
      fetchCapabilities();
    } catch (error) {
      console.error('Error removing architecture diagram:', error);
      showNotification(
        'error',
        'Remove failed',
        error.response?.data?.message || error.message
      );
    } finally {
      setArchitectureRemoving(false);
    }
  };

  const activeCapabilities = capabilities.filter((capability) => capability.isActive).length;
  const totalTestCases = Object.values(testCaseCounts).reduce(
    (sum, count) => sum + (Number.isFinite(count) ? count : 0),
    0
  );

  const getPriorityTag = (priority) => {
    const typeMap = {
      CRITICAL: 'red',
      HIGH: 'magenta',
      MEDIUM: 'purple',
      LOW: 'cool-gray',
    };
    const normalized = priority || 'MEDIUM';
    return <Tag type={typeMap[normalized] || 'gray'}>{normalized}</Tag>;
  };

  const testCaseHeaders = [
    { key: 'name', header: 'Test Case' },
    { key: 'priority', header: 'Priority' },
    { key: 'description', header: 'Description' },
    { key: 'expected', header: 'Expected Behavior' },
    { key: 'actions', header: 'Actions' },
  ];

  const testCaseRows = testCases.map((testCase) => ({
    id: testCase.id,
    name: testCase.testCaseName || 'N/A',
    priority: getPriorityTag(testCase.priority),
    description: <div className="testcase-cell">{testCase.description || '—'}</div>,
    expected: <div className="testcase-cell">{testCase.expectedBehavior || '—'}</div>,
    actions: (
      <div className="capability-actions">
        <Button kind="ghost" size="sm" onClick={() => openEditTestCase(testCase)}>
          Edit
        </Button>
        <Button
          kind="danger--ghost"
          size="sm"
          onClick={() => handleDeleteTestCase(testCase.id)}
        >
          Delete
        </Button>
      </div>
    ),
  }));

  const headers = [
    { key: 'testName', header: 'Test Name' },
    { key: 'capability', header: 'Capability' },
    { key: 'testType', header: 'Test Type' },
    { key: 'buildNumber', header: 'Build Number' },
    { key: 'uploadDate', header: 'Upload Date' },
    { key: 'status', header: 'Status' },
    { key: 'actions', header: 'Actions' },
  ];

  const getStatusTag = (status) => {
    const statusMap = {
      COMPLETED: 'green',
      PROCESSING: 'gray',
      FAILED: 'red',
    };
    return <Tag type={statusMap[status] || 'gray'}>{status || 'N/A'}</Tag>;
  };

  const rows = filteredRuns.map((test) => ({
    id: test.id,
    testName: test.testName || 'N/A',
    capability: <Tag type="gray">{test.capability || 'N/A'}</Tag>,
    testType: <Tag type="gray">{test.testType || 'N/A'}</Tag>,
    buildNumber: test.buildNumber || 'N/A',
    uploadDate: test.uploadDate ? dayjs(test.uploadDate).format('YYYY-MM-DD HH:mm') : 'N/A',
    status: getStatusTag(test.status),
    actions: (
      <div className="capability-actions">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Edit}
          onClick={() => navigate(`/report-builder/${test.id}`)}
        >
          Add details
        </Button>
        <Button
          kind="ghost"
          size="sm"
          renderIcon={DocumentBlank}
          onClick={() => navigate(`/report-builder/${test.id}`)}
        >
          Open builder
        </Button>
      </div>
    ),
  }));

  return (
    <div className="capability-details page-container fade-in">
      <Tile className="capability-hero">
        <div className="capability-hero-inner">
          <div className="capability-hero-title">
            <div className="capability-hero-icon">
              <Layers size={40} />
            </div>
            <div>
              <h1>Capability details</h1>
              <p>Capture narrative context that powers report generation.</p>
            </div>
          </div>
          <div className="capability-hero-actions">
            <Button kind="primary" renderIcon={Add} onClick={() => setCreateOpen(true)}>
              New capability
            </Button>
            <Button kind="tertiary" renderIcon={Renew} onClick={refreshAll}>
              Refresh
            </Button>
          </div>
        </div>
      </Tile>

      {notification && (
        <div className="capability-alert">
          <InlineNotification
            kind={notification.kind}
            title={notification.title}
            subtitle={notification.subtitle}
            onClose={() => setNotification(null)}
            lowContrast
          />
        </div>
      )}

      <Grid className="capability-summary" narrow>
        <Column sm={4} md={4} lg={3}>
          <Tile className="summary-tile">
            <p className="summary-label">Total capabilities</p>
            <p className="summary-value">{capabilities.length}</p>
          </Tile>
        </Column>
        <Column sm={4} md={4} lg={3}>
          <Tile className="summary-tile">
            <p className="summary-label">Active capabilities</p>
            <p className="summary-value">{activeCapabilities}</p>
          </Tile>
        </Column>
        <Column sm={4} md={4} lg={3}>
          <Tile className="summary-tile">
            <p className="summary-label">Filtered test runs</p>
            <p className="summary-value">{filteredRuns.length}</p>
          </Tile>
        </Column>
        <Column sm={4} md={4} lg={3}>
          <Tile className="summary-tile">
            <p className="summary-label">Total test cases</p>
            <p className="summary-value">{loadingTestCaseCounts ? '—' : totalTestCases}</p>
          </Tile>
        </Column>
      </Grid>

      <Tile className="capability-filter-panel">
        <Grid narrow>
          <Column sm={4} md={4} lg={6}>
            <Search
              labelText="Search"
              placeholder="Search by test name, capability, or build"
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
              disabled={loadingCapabilities || capabilityOptions.length === 1}
            >
              {capabilityOptions.map((capability) => (
                <SelectItem
                  key={capability}
                  value={capability}
                  text={capability === 'all' ? 'All capabilities' : capability}
                />
              ))}
            </Select>
          </Column>
          <Column sm={4} md={2} lg={3}>
            <Select
              id="testtype-filter"
              labelText="Test type"
              value={filterTestType}
              onChange={(e) => setFilterTestType(e.target.value)}
            >
              {testTypeOptions.map((type) => (
                <SelectItem key={type} value={type} text={type === 'all' ? 'All types' : type} />
              ))}
            </Select>
          </Column>
          <Column sm={4} md={2} lg={4}>
            <Select
              id="status-filter"
              labelText="Status"
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
            >
              {statusOptions.map((status) => (
                <SelectItem
                  key={status}
                  value={status}
                  text={status === 'all' ? 'All statuses' : status}
                />
              ))}
            </Select>
          </Column>
        </Grid>
      </Tile>

      <Tile className="capability-table">
        <div className="capability-table-header">
          <h3>Choose a test run to add narrative details</h3>
          <p>These details flow into the report builder and are stored with each test run.</p>
        </div>
        {loading ? (
          <Loading description="Loading test runs..." withOverlay={false} />
        ) : filteredRuns.length === 0 ? (
          <div className="capability-empty">
            <p>No test runs match the current filters.</p>
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

      <Tile className="capability-document-data">
        <div className="capability-table-header">
          <h3>Test run narrative data</h3>
          <p>Save the report builder narrative sections directly from this page.</p>
        </div>
        <div className="metadata-controls">
          <Select
            id="capability-document-data-select"
            labelText="Select test run"
            value={documentRunId}
            onChange={(e) => setDocumentRunId(e.target.value)}
            disabled={loading || testRuns.length === 0}
          >
            {testRuns.length === 0 ? (
              <SelectItem value="" text="No test runs available" />
            ) : (
              testRuns.map((test) => (
                <SelectItem
                  key={test.id}
                  value={String(test.id)}
                  text={`${test.testName || 'Test Run'} · ${test.capability || 'Unknown capability'} (#${test.id})`}
                />
              ))
            )}
          </Select>
          <Button
            kind="primary"
            onClick={handleSaveDocumentData}
            disabled={!documentRunId || documentSaving || documentLoading}
          >
            {documentSaving ? 'Saving...' : 'Save narrative data'}
          </Button>
        </div>
        {documentLoading ? (
          <Loading description="Loading narrative data..." withOverlay={false} />
        ) : (
          <div className="document-data-form">
            <div className="document-data-grid">
              <TextInput
                id="document-capability-name"
                labelText="Capability name"
                value={documentForm.capabilityName}
                onChange={(e) => handleDocumentChange('capabilityName', e.target.value)}
                placeholder="APIC-GQL"
              />
              <TextInput
                id="document-description"
                labelText="Description"
                value={documentForm.description}
                onChange={(e) => handleDocumentChange('description', e.target.value)}
                placeholder="Demo report for APIC-GQL Capability."
              />
            </div>
            <TextArea
              id="document-introduction"
              labelText="Introduction (one item per line)"
              helperText="Each line becomes a separate paragraph."
              value={documentForm.introduction}
              onChange={(e) => handleDocumentChange('introduction', e.target.value)}
              rows={4}
            />
            <TextArea
              id="document-benchmark-goals"
              labelText="Benchmark goals (one item per line)"
              helperText="Each line becomes a separate bullet."
              value={documentForm.benchmarkGoals}
              onChange={(e) => handleDocumentChange('benchmarkGoals', e.target.value)}
              rows={4}
            />
            <TextArea
              id="document-test-setup"
              labelText="Test setup (one item per line)"
              helperText="Example: JMeter 5.6.3, Heap 4GB."
              value={documentForm.testSetup}
              onChange={(e) => handleDocumentChange('testSetup', e.target.value)}
              rows={4}
            />
            <TextArea
              id="document-hardware-info"
              labelText="Hardware info (one item per line)"
              helperText="Example: App nodes: 16 vCPU, 32GB RAM."
              value={documentForm.hardwareInfo}
              onChange={(e) => handleDocumentChange('hardwareInfo', e.target.value)}
              rows={4}
            />
            <div className="document-scenarios">
              <div className="document-scenarios-header">
                <h4>Scenarios (one item per line)</h4>
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={addScenarioField}
                  disabled={documentForm.scenarioFields.length >= 10}
                >
                  Add scenario
                </Button>
              </div>
              {documentForm.scenarioFields.map((scenario, index) => (
                <div className="document-scenario" key={`scenario-${index}`}>
                  <TextArea
                    id={`document-scenario-${index + 1}`}
                    labelText={`Scenario ${index + 1}`}
                    value={scenario}
                    onChange={(e) => handleScenarioChange(index, e.target.value)}
                    rows={4}
                  />
                  {documentForm.scenarioFields.length > 1 && (
                    <Button
                      kind="ghost"
                      size="sm"
                      onClick={() => removeScenarioField(index)}
                    >
                      Remove
                    </Button>
                  )}
                </div>
              ))}
            </div>
            <TextArea
              id="document-performance-analysis"
              labelText="Performance analysis (one item per line)"
              value={documentForm.performanceAnalysis}
              onChange={(e) => handleDocumentChange('performanceAnalysis', e.target.value)}
              rows={4}
            />
            <TextArea
              id="document-capacity-planning"
              labelText="Capacity planning (one item per line)"
              value={documentForm.capacityPlanning}
              onChange={(e) => handleDocumentChange('capacityPlanning', e.target.value)}
              rows={4}
            />
            <TextArea
              id="document-conclusions"
              labelText="Conclusions (one item per line)"
              value={documentForm.conclusions}
              onChange={(e) => handleDocumentChange('conclusions', e.target.value)}
              rows={4}
            />
            <TextArea
              id="document-additional-notes"
              labelText="Additional notes (one item per line)"
              value={documentForm.additionalNotes}
              onChange={(e) => handleDocumentChange('additionalNotes', e.target.value)}
              rows={4}
            />
          </div>
        )}
      </Tile>

      <Tile className="capability-testcases">
        <div className="capability-table-header">
          <h3>Capability test cases</h3>
          <p>Maintain test case descriptions used in capability reporting.</p>
        </div>
        <div className="testcase-controls">
          <Select
            id="capability-testcase-select"
            labelText="Select capability"
            value={selectedCapabilityId}
            onChange={(e) => setSelectedCapabilityId(e.target.value)}
            disabled={loadingCapabilities || capabilities.length === 0}
          >
            {capabilities.length === 0 ? (
              <SelectItem value="" text="No capabilities available" />
            ) : (
              capabilities.map((capability) => (
                <SelectItem
                  key={capability.id}
                  value={String(capability.id)}
                  text={`${capability.name} (${loadingTestCaseCounts ? '…' : (testCaseCounts[capability.id] ?? 0)})`}
                />
              ))
            )}
          </Select>
          <Button
            kind="primary"
            renderIcon={Add}
            onClick={openCreateTestCase}
            disabled={!selectedCapabilityId}
          >
            Add test case
          </Button>
        </div>

        {loadingTestCases ? (
          <Loading description="Loading test cases..." withOverlay={false} />
        ) : testCaseRows.length === 0 ? (
          <div className="capability-empty">
            <p>No test cases found for this capability.</p>
          </div>
        ) : (
          <DataTable rows={testCaseRows} headers={testCaseHeaders}>
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

      <Tile className="capability-metadata">
        <div className="capability-table-header">
          <h3>Capability objective, scope, and environment</h3>
          <p>These values appear on the report cover page and are required for generation.</p>
        </div>
        <div className="metadata-controls">
          <Select
            id="capability-metadata-select"
            labelText="Select capability"
            value={selectedCapabilityId}
            onChange={(e) => setSelectedCapabilityId(e.target.value)}
            disabled={loadingCapabilities || capabilities.length === 0}
          >
            {capabilities.length === 0 ? (
              <SelectItem value="" text="No capabilities available" />
            ) : (
              capabilities.map((capability) => (
                <SelectItem
                  key={capability.id}
                  value={String(capability.id)}
                  text={capability.name}
                />
              ))
            )}
          </Select>
          <Button
            kind="primary"
            onClick={handleSaveMetadata}
            disabled={!selectedCapabilityId || metadataSaving}
          >
            {metadataSaving ? 'Saving...' : 'Save details'}
          </Button>
        </div>
        <div className="capability-modal">
          <TextArea
            id="capability-objective"
            labelText="Test objective"
            placeholder="Describe the performance objective"
            value={metadataForm.testObjective}
            onChange={(e) => handleMetadataChange('testObjective', e.target.value)}
            rows={4}
          />
          <TextArea
            id="capability-scope"
            labelText="Test scope"
            placeholder="Define what is included in scope"
            value={metadataForm.testScope}
            onChange={(e) => handleMetadataChange('testScope', e.target.value)}
            rows={4}
          />
          <TextArea
            id="capability-environment"
            labelText="Environment details"
            placeholder="Hardware, platform, and environment configuration"
            value={metadataForm.environmentDetails}
            onChange={(e) => handleMetadataChange('environmentDetails', e.target.value)}
            rows={4}
          />
        </div>
      </Tile>

      <Tile className="capability-architecture">
        <div className="capability-table-header">
          <h3>Capability architecture diagram</h3>
          <p>Store a diagram used across reports for this capability.</p>
        </div>
        <div className="metadata-controls">
          <Select
            id="capability-architecture-select"
            labelText="Select capability"
            value={selectedCapabilityId}
            onChange={(e) => setSelectedCapabilityId(e.target.value)}
            disabled={loadingCapabilities || capabilities.length === 0}
          >
            {capabilities.length === 0 ? (
              <SelectItem value="" text="No capabilities available" />
            ) : (
              capabilities.map((capability) => (
                <SelectItem
                  key={capability.id}
                  value={String(capability.id)}
                  text={capability.name}
                />
              ))
            )}
          </Select>
          <Button
            kind="primary"
            renderIcon={Upload}
            onClick={handleUploadArchitectureDiagram}
            disabled={!selectedCapabilityId || architectureSaving || !architectureFile}
          >
            {architectureSaving ? 'Uploading...' : 'Upload diagram'}
          </Button>
        </div>
        <div className="architecture-uploader">
          <FileUploader
            labelTitle="Upload diagram"
            labelDescription="PNG, JPG, or SVG. Max 10MB."
            buttonLabel="Add file"
            buttonKind="secondary"
            size="lg"
            filenameStatus="edit"
            accept={['.png', '.jpg', '.jpeg', '.svg']}
            multiple={false}
            disabled={architectureSaving || Boolean(architectureFile)}
            onChange={handleArchitectureFileChange}
          />
          {architectureFile && (
            <div className="architecture-file">
              <CheckmarkFilled size={20} style={{ color: '#24a148' }} />
              <span className="architecture-file-name">{architectureFile.name}</span>
              <span className="architecture-file-meta">
                ({(architectureFile.size / 1024 / 1024).toFixed(2)} MB)
              </span>
              <Button
                kind="ghost"
                size="sm"
                onClick={clearArchitectureFile}
                disabled={architectureSaving}
              >
                Remove
              </Button>
            </div>
          )}
          {selectedCapability?.architectureDiagramPath && (
            <div className="architecture-current">
              <span className="architecture-current-label">Current diagram:</span>
              <span>
                {selectedCapability.architectureDiagramPath.split(/[/\\]/).pop()}
              </span>
            </div>
          )}
          {selectedCapability?.architectureDiagramPath && (
            <div className="architecture-actions">
              <Button
                kind="ghost"
                size="sm"
                renderIcon={View}
                onClick={() => window.open(architecturePreviewUrl, '_blank', 'noopener,noreferrer')}
              >
                Preview
              </Button>
              <Button
                kind="ghost"
                size="sm"
                renderIcon={Download}
                onClick={() => {
                  window.location.href = architectureDownloadUrl;
                }}
              >
                Download
              </Button>
              <Button
                kind="danger--ghost"
                size="sm"
                renderIcon={TrashCan}
                onClick={handleRemoveArchitectureDiagram}
                disabled={architectureRemoving}
              >
                {architectureRemoving ? 'Removing...' : 'Remove'}
              </Button>
            </div>
          )}
          {selectedCapability?.architectureDiagramPath && (
            <div className="architecture-preview">
              <img
                src={architecturePreviewUrl}
                alt="Architecture diagram preview"
                loading="lazy"
              />
            </div>
          )}
        </div>
      </Tile>

      <Tile className="capability-baseline">
        <div className="capability-table-header">
          <h3>Baseline metrics (per test case label)</h3>
          <p>Used to evaluate pass/fail during report generation.</p>
        </div>
        <div className="metadata-controls">
          <Select
            id="capability-baseline-select"
            labelText="Select capability"
            value={selectedCapabilityId}
            onChange={(e) => setSelectedCapabilityId(e.target.value)}
            disabled={loadingCapabilities || capabilities.length === 0}
          >
            {capabilities.length === 0 ? (
              <SelectItem value="" text="No capabilities available" />
            ) : (
              capabilities.map((capability) => (
                <SelectItem
                  key={capability.id}
                  value={String(capability.id)}
                  text={capability.name}
                />
              ))
            )}
          </Select>
          <Button
            kind="primary"
            onClick={handleSaveBaseline}
            disabled={!selectedCapabilityId || baselineSaving}
          >
            {baselineSaving ? 'Saving...' : 'Save baseline'}
          </Button>
        </div>
        <div className="baseline-grid">
          <TextInput
            id="baseline-p95"
            type="number"
            labelText="P95 max (ms)"
            value={baselineForm.p95MaxMs}
            onChange={(e) => handleBaselineChange('p95MaxMs', e.target.value)}
            min={0}
            step="0.01"
          />
          <TextInput
            id="baseline-avg"
            type="number"
            labelText="Avg max (ms)"
            value={baselineForm.avgMaxMs}
            onChange={(e) => handleBaselineChange('avgMaxMs', e.target.value)}
            min={0}
            step="0.01"
          />
          <TextInput
            id="baseline-p90"
            type="number"
            labelText="P90 max (ms)"
            value={baselineForm.p90MaxMs}
            onChange={(e) => handleBaselineChange('p90MaxMs', e.target.value)}
            min={0}
            step="0.01"
          />
          <TextInput
            id="baseline-throughput"
            type="number"
            labelText="Throughput min (req/s)"
            value={baselineForm.throughputMin}
            onChange={(e) => handleBaselineChange('throughputMin', e.target.value)}
            min={0}
            step="0.01"
          />
        </div>
      </Tile>

      <Tile className="capability-guidance">
        <div>
          <h3>What to capture</h3>
          <ul>
            <li>Capability description, scope, and business intent.</li>
            <li>Test harness details, scenarios, and infrastructure.</li>
            <li>Performance analysis, conclusions, and tuning notes.</li>
          </ul>
        </div>
        <div>
          <h3>How it is used</h3>
          <ul>
            <li>Saved details are attached to each test run.</li>
            <li>Report generation uses the saved narrative sections.</li>
            <li>Update anytime to refresh report content.</li>
          </ul>
        </div>
      </Tile>

      <Modal
        open={createOpen}
        modalHeading="Create capability"
        primaryButtonText={createSaving ? 'Creating...' : 'Create capability'}
        secondaryButtonText="Cancel"
        onRequestClose={() => {
          setCreateOpen(false);
          resetCreateForm();
        }}
        onRequestSubmit={handleCreateCapability}
        primaryButtonDisabled={createSaving}
      >
        <div className="capability-modal">
          <TextInput
            id="capability-name"
            labelText="Capability name"
            placeholder="e.g., Payments, Messaging"
            value={createForm.name}
            onChange={(e) => handleCreateChange('name', e.target.value)}
            invalid={createTouched && !createForm.name.trim()}
            invalidText="Capability name is required"
          />
          <TextArea
            id="capability-description"
            labelText="Description"
            placeholder="Describe the capability scope and purpose"
            value={createForm.description}
            onChange={(e) => handleCreateChange('description', e.target.value)}
            rows={4}
          />
        </div>
      </Modal>

      <Modal
        open={testCaseModalOpen}
        modalHeading={editingTestCase ? 'Edit test case' : 'Add test case'}
        primaryButtonText={testCaseSaving ? 'Saving...' : 'Save test case'}
        secondaryButtonText="Cancel"
        onRequestClose={() => {
          setTestCaseModalOpen(false);
          resetTestCaseForm();
        }}
        onRequestSubmit={handleSaveTestCase}
        primaryButtonDisabled={testCaseSaving}
      >
        <div className="capability-modal">
          <TextInput
            id="testcase-name"
            labelText="Test case name"
            placeholder="e.g., Baseline throughput"
            value={testCaseForm.testCaseName}
            onChange={(e) => handleTestCaseChange('testCaseName', e.target.value)}
            invalid={testCaseTouched && !testCaseForm.testCaseName.trim()}
            invalidText="Test case name is required"
          />
          <Select
            id="testcase-priority"
            labelText="Priority"
            value={testCaseForm.priority}
            onChange={(e) => handleTestCaseChange('priority', e.target.value)}
          >
            {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((priority) => (
              <SelectItem key={priority} value={priority} text={priority} />
            ))}
          </Select>
          <TextArea
            id="testcase-description"
            labelText="Description"
            placeholder="Describe the scenario and validation steps"
            value={testCaseForm.description}
            onChange={(e) => handleTestCaseChange('description', e.target.value)}
            rows={4}
          />
          <TextArea
            id="testcase-expected"
            labelText="Expected behavior"
            placeholder="Define expected performance criteria"
            value={testCaseForm.expectedBehavior}
            onChange={(e) => handleTestCaseChange('expectedBehavior', e.target.value)}
            rows={4}
          />
        </div>
      </Modal>
    </div>
  );
};

export default CapabilityDetails;

// Made with Bob
