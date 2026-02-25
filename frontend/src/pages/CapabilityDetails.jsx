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
import { Renew, Layers, Add, CheckmarkFilled, Upload, View, Download, TrashCan } from '@carbon/icons-react';
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
    instanaNamespace: '',
    instanaEntityType: '',
    instanaQuery: '',
    podCpuMetric: '',
    podMemoryMetric: '',
    jvmHeapUsedMetric: '',
    jvmGcPauseMetric: '',
    jvmProcessCpuMetric: '',
  });
  const [metadataSaving, setMetadataSaving] = useState(false);
  const [baselineForm, setBaselineForm] = useState({
    p95MaxMs: '',
    avgMaxMs: '',
    p90MaxMs: '',
    throughputMin: '',
  });
  const [baselineSaving, setBaselineSaving] = useState(false);
  const [slaForm, setSlaForm] = useState([]);
  const [slaSaving, setSlaSaving] = useState(false);
  const [selectedSlaLabel, setSelectedSlaLabel] = useState('');
  const [slaLabels, setSlaLabels] = useState([]);
  const [architectureFile, setArchitectureFile] = useState(null);
  const [architectureSaving, setArchitectureSaving] = useState(false);
  const [architectureRemoving, setArchitectureRemoving] = useState(false);
  const [architecturePreviewDataUrl, setArchitecturePreviewDataUrl] = useState(null);
  const [batches, setBatches] = useState([]);
  const [loadingBatches, setLoadingBatches] = useState(false);
  const [batchGenerating, setBatchGenerating] = useState({});
  const [batchDetailsById, setBatchDetailsById] = useState({});
  const [batchDetailsLoading, setBatchDetailsLoading] = useState({});
  const [batchReportTypesInput, setBatchReportTypesInput] = useState(
    'TECHNICAL_WORD, TECHNICAL_PDF, RAW_DATA_CSV'
  );

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
      setMetadataForm({
        testObjective: '',
        testScope: '',
        environmentDetails: '',
        instanaNamespace: '',
        instanaEntityType: '',
        instanaQuery: '',
        podCpuMetric: '',
        podMemoryMetric: '',
        jvmHeapUsedMetric: '',
        jvmGcPauseMetric: '',
        jvmProcessCpuMetric: '',
      });
      setBaselineForm({ p95MaxMs: '', avgMaxMs: '', p90MaxMs: '', throughputMin: '' });
      setSlaForm([]);
      setArchitectureFile(null);
      return;
    }

    const selected = capabilities.find(
      (capability) => String(capability.id) === String(selectedCapabilityId)
    );
    const instanaConfig = selected?.instanaConfig || {};
    setMetadataForm({
      testObjective: selected?.testObjective || '',
      testScope: selected?.testScope || '',
      environmentDetails: selected?.environmentDetails || '',
      instanaNamespace: instanaConfig.namespace || '',
      instanaEntityType: instanaConfig.entityType || '',
      instanaQuery: instanaConfig.query || '',
      podCpuMetric: instanaConfig.podCpuMetric || '',
      podMemoryMetric: instanaConfig.podMemoryMetric || '',
      jvmHeapUsedMetric: instanaConfig.jvmHeapUsedMetric || '',
      jvmGcPauseMetric: instanaConfig.jvmGcPauseMetric || '',
      jvmProcessCpuMetric: instanaConfig.jvmProcessCpuMetric || '',
    });
    loadSlaCriteria(selected);
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
    if (selectedCapabilityId) {
      fetchBatches(selectedCapabilityId);
    } else {
      setBatches([]);
      setBatchDetailsById({});
    }
  }, [selectedCapabilityId]);

  useEffect(() => {
    const fetchArchitecturePreview = async () => {
      if (!selectedCapabilityId) {
        setArchitecturePreviewDataUrl(null);
        return;
      }

      try {
        const response = await axios.get(
          API_ENDPOINTS.CAPABILITY_ARCHITECTURE_PREVIEW(selectedCapabilityId),
          { responseType: 'blob' }
        );
        const blob = response.data;
        const url = URL.createObjectURL(blob);
        setArchitecturePreviewDataUrl(url);
      } catch (error) {
        console.error('Error fetching architecture preview:', error);
        setArchitecturePreviewDataUrl(null);
      }
    };

    fetchArchitecturePreview();

    return () => {
      if (architecturePreviewDataUrl) {
        URL.revokeObjectURL(architecturePreviewDataUrl);
      }
    };
  }, [selectedCapabilityId]);

  const refreshAll = () => {
    fetchTestRuns();
    fetchCapabilities();
    fetchTestCaseCounts();
    if (selectedCapabilityId) {
      fetchBatches(selectedCapabilityId);
    }
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

  const loadSlaCriteria = (capabilityData) => {
    const acceptanceCriteria = capabilityData?.acceptanceCriteria || {};
    const labelCriteria = acceptanceCriteria.labelCriteria || {};
    const labelsArray = Object.keys(labelCriteria);

    setSlaLabels(labelsArray);

    if (labelsArray.length > 0) {
      const firstLabel = labelsArray[0];
      setSelectedSlaLabel(firstLabel);
      setSlaForm(labelCriteria[firstLabel] || []);
    } else {
      setSelectedSlaLabel('');
      setSlaForm([]);
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

  const parseBatchReportTypes = () => {
    const types = batchReportTypesInput
      .split(',')
      .map((type) => type.trim().toUpperCase())
      .filter(Boolean);

    return types.length > 0
      ? [...new Set(types)]
      : ['TECHNICAL_WORD', 'TECHNICAL_PDF', 'RAW_DATA_CSV'];
  };

  const canGenerateBatchReport = (status) => {
    return status === 'COMPLETED' || status === 'FAILED';
  };

  const fetchBatches = async (capabilityId) => {
    if (!capabilityId) {
      setBatches([]);
      return;
    }

    setLoadingBatches(true);
    try {
      const response = await axios.get(API_ENDPOINTS.BATCH_TESTS_BY_CAPABILITY(capabilityId));
      const data = response.data?.data || [];
      setBatches(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Error fetching batches:', error);
      showNotification(
        'error',
        'Failed to load batches',
        error.response?.data?.message || error.message
      );
      setBatches([]);
    } finally {
      setLoadingBatches(false);
    }
  };

  const handleLoadBatchDetails = async (batchId) => {
    setBatchDetailsLoading((prev) => ({ ...prev, [batchId]: true }));
    try {
      const response = await axios.get(API_ENDPOINTS.BATCH_TEST_STATUS(batchId));
      const batchData = response.data?.data;
      if (batchData) {
        setBatchDetailsById((prev) => ({
          ...prev,
          [batchId]: batchData,
        }));
      }
    } catch (error) {
      console.error('Error loading batch details:', error);
      showNotification(
        'error',
        'Failed to load batch details',
        error.response?.data?.message || error.message
      );
    } finally {
      setBatchDetailsLoading((prev) => ({ ...prev, [batchId]: false }));
    }
  };

  const handleGenerateBatchReports = async (batchId) => {
    setBatchGenerating((prev) => ({ ...prev, [batchId]: true }));
    try {
      const response = await axios.post(
        API_ENDPOINTS.BATCH_TEST_GENERATE_REPORT(batchId),
        { reportTypes: parseBatchReportTypes() }
      );

      const batchData = response.data?.data;
      if (batchData) {
        setBatchDetailsById((prev) => ({
          ...prev,
          [batchId]: batchData,
        }));
        setBatches((prev) =>
          prev.map((batch) =>
            batch.batchId === batchId
              ? {
                  ...batch,
                  status: batchData.status,
                  batchResult: batchData.batchResult,
                  completedTestCases: batchData.completedTestCases,
                  failedTestCases: batchData.failedTestCases,
                  totalTestCases: batchData.totalTestCases,
                  progressPercentage: batchData.progressPercentage,
                }
              : batch
          )
        );
      }

      showNotification('success', 'Batch reports generated', `Reports created for batch ${batchId}`);
    } catch (error) {
      console.error('Error generating batch reports:', error);
      showNotification(
        'error',
        'Batch report generation failed',
        error.response?.data?.message || error.message
      );
    } finally {
      setBatchGenerating((prev) => ({ ...prev, [batchId]: false }));
    }
  };

  const handleDownloadBatchReport = async (reportId, reportType) => {
    try {
      const response = await axios.get(API_ENDPOINTS.REPORT_DOWNLOAD(reportId), {
        responseType: 'blob',
      });

      const disposition = response.headers?.['content-disposition'] || '';
      const matchedName = disposition.match(/filename=\"?([^\";]+)\"?/i)?.[1];
      const fallback = reportType ? `${reportType.toLowerCase()}-${reportId}` : `report-${reportId}`;
      const resolvedName = matchedName || fallback;

      const blob = new Blob([response.data], {
        type: response.headers?.['content-type'] || 'application/octet-stream',
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', resolvedName);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Error downloading batch report:', error);
      showNotification('error', 'Download failed', error.response?.data?.message || 'Failed to download batch report');
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

  const addSlaCriteria = () => {
    setSlaForm([
      ...slaForm,
      { label: '', threshold: '', unit: 'ms', operator: '<' },
    ]);
  };

  const updateSlaCriteria = (index, field, value) => {
    const updated = [...slaForm];
    updated[index] = { ...updated[index], [field]: value };
    setSlaForm(updated);
  };

  const removeSlaCriteria = (index) => {
    setSlaForm(slaForm.filter((_, i) => i !== index));
  };

  const handleSlaLabelChange = (newLabel) => {
    setSelectedSlaLabel(newLabel);
    // Load criteria for the selected label from current capability
    const selected = capabilities.find(
      (capability) => String(capability.id) === String(selectedCapabilityId)
    );
    if (selected) {
      const labelCriteria = selected.acceptanceCriteria?.labelCriteria || {};
      setSlaForm(labelCriteria[newLabel] || []);
    }
  };

  const handleAddLabel = async () => {
    const newLabel = window.prompt('Enter new test label/transaction name:');
    if (!newLabel || !newLabel.trim()) return;

    const trimmedLabel = newLabel.trim();
    if (slaLabels.includes(trimmedLabel)) {
      showNotification('error', 'Label exists', `Label "${trimmedLabel}" already defined`);
      return;
    }

    const updatedLabels = [...slaLabels, trimmedLabel];
    setSlaLabels(updatedLabels);
    setSelectedSlaLabel(trimmedLabel);
    setSlaForm([]);
  };

  const handleSaveSla = async () => {
    if (!selectedCapabilityId) {
      showNotification('error', 'No capability selected', 'Select a capability first');
      return;
    }

    if (!selectedSlaLabel) {
      showNotification('error', 'No label selected', 'Select a test label first');
      return;
    }

    setSlaSaving(true);
    try {
      // Build labelCriteria object with all labels
      const labelCriteria = {};
      slaLabels.forEach((label) => {
        if (label === selectedSlaLabel) {
          // Save the current edited criteria
          labelCriteria[label] = slaForm.filter((c) => c.label && c.threshold);
        } else {
          // Keep existing criteria or empty array
          labelCriteria[label] = [];
        }
      });

      const payload = {
        testObjective: metadataForm.testObjective || null,
        testScope: metadataForm.testScope || null,
        environmentDetails: metadataForm.environmentDetails || null,
        acceptanceCriteria: {
          labelCriteria,
        },
      };

      await axios.put(API_ENDPOINTS.CAPABILITY_METADATA(selectedCapabilityId), payload);
      showNotification('success', 'SLA criteria saved', `Acceptance criteria updated for label: ${selectedSlaLabel}`);
      fetchCapabilities();
    } catch (error) {
      console.error('Error saving SLA criteria:', error);
      showNotification(
        'error',
        'Save failed',
        error.response?.data?.message || error.message
      );
    } finally {
      setSlaSaving(false);
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
          instanaConfig: {
            namespace: metadataForm.instanaNamespace.trim(),
            entityType: metadataForm.instanaEntityType.trim(),
            query: metadataForm.instanaQuery.trim(),
            podCpuMetric: metadataForm.podCpuMetric.trim(),
            podMemoryMetric: metadataForm.podMemoryMetric.trim(),
            jvmHeapUsedMetric: metadataForm.jvmHeapUsedMetric.trim(),
            jvmGcPauseMetric: metadataForm.jvmGcPauseMetric.trim(),
            jvmProcessCpuMetric: metadataForm.jvmProcessCpuMetric.trim(),
          },
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
      setArchitecturePreviewDataUrl(null);
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
  }));

  const batchHeaders = [
    { key: 'batchName', header: 'Batch Name' },
    { key: 'batchId', header: 'Batch ID' },
    { key: 'status', header: 'Status' },
    { key: 'progress', header: 'Progress' },
    { key: 'result', header: 'Result' },
    { key: 'reports', header: 'Reports' },
    { key: 'actions', header: 'Actions' },
  ];

  const batchRows = batches.map((batch) => {
    const detail = batchDetailsById[batch.batchId];
    const reports = detail?.consolidatedReports || [];
    const total = batch.totalTestCases || 0;
    const completed = batch.completedTestCases || 0;
    const progressValue = Number.isFinite(batch.progressPercentage)
      ? Number(batch.progressPercentage).toFixed(1)
      : '0.0';

    return {
      id: batch.batchId,
      batchName: batch.batchName || 'N/A',
      batchId: batch.batchId,
      status: getStatusTag(batch.status),
      progress: `${completed}/${total} (${progressValue}%)`,
      result: <Tag type={batch.batchResult === 'PASS' ? 'green' : 'gray'}>{batch.batchResult || 'N/A'}</Tag>,
      reports:
        reports.length > 0 ? (
          <div className="batch-report-list">
            {reports.map((report) => (
              <Button
                key={report.id}
                kind="ghost"
                size="sm"
                onClick={() => handleDownloadBatchReport(report.id, report.reportType)}
              >
                {report.reportType}
              </Button>
            ))}
          </div>
        ) : (
          <span className="batch-no-reports">No reports loaded</span>
        ),
      actions: (
        <div className="capability-actions">
          <Button
            kind="ghost"
            size="sm"
            onClick={() => handleLoadBatchDetails(batch.batchId)}
            disabled={batchDetailsLoading[batch.batchId]}
          >
            {batchDetailsLoading[batch.batchId] ? 'Loading...' : 'View reports'}
          </Button>
          <Button
            kind="primary"
            size="sm"
            onClick={() => handleGenerateBatchReports(batch.batchId)}
            disabled={batchGenerating[batch.batchId] || !canGenerateBatchReport(batch.status)}
          >
            {batchGenerating[batch.batchId] ? 'Generating...' : 'Generate reports'}
          </Button>
        </div>
      ),
    };
  });

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
              <p>Manage capability metadata, test cases, batch reporting, and baselines.</p>
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
          <h3>Test runs</h3>
          <p>Browse uploaded test runs and filter by capability, type, and status.</p>
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

      <Tile className="capability-batch-reporting">
        <div className="capability-table-header">
          <h3>Batch reporting</h3>
          <p>Generate and download consolidated reports for completed batches.</p>
        </div>
        <div className="metadata-controls">
          <Select
            id="capability-batch-select"
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
          <TextInput
            id="batch-report-types"
            labelText="Report types (comma-separated)"
            value={batchReportTypesInput}
            onChange={(e) => setBatchReportTypesInput(e.target.value)}
            placeholder="TECHNICAL_WORD, TECHNICAL_PDF, RAW_DATA_CSV"
          />
          <Button
            kind="tertiary"
            onClick={() => fetchBatches(selectedCapabilityId)}
            disabled={!selectedCapabilityId || loadingBatches}
          >
            {loadingBatches ? 'Refreshing...' : 'Refresh batches'}
          </Button>
        </div>

        {loadingBatches ? (
          <Loading description="Loading batches..." withOverlay={false} />
        ) : batchRows.length === 0 ? (
          <div className="capability-empty">
            <p>No batches found for this capability.</p>
          </div>
        ) : (
          <DataTable rows={batchRows} headers={batchHeaders}>
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
          <TextInput
            id="instana-namespace"
            labelText="Instana namespace"
            placeholder="e.g., prod-platform"
            value={metadataForm.instanaNamespace}
            onChange={(e) => handleMetadataChange('instanaNamespace', e.target.value)}
          />
          <TextInput
            id="instana-entity-type"
            labelText="Instana entity type"
            placeholder="e.g., kubernetes.pod"
            value={metadataForm.instanaEntityType}
            onChange={(e) => handleMetadataChange('instanaEntityType', e.target.value)}
          />
          <TextInput
            id="instana-query"
            labelText="Instana query / selector"
            placeholder="e.g., kubernetes.label.app:my-service"
            value={metadataForm.instanaQuery}
            onChange={(e) => handleMetadataChange('instanaQuery', e.target.value)}
          />
          <TextInput
            id="instana-pod-cpu-metric"
            labelText="Pod CPU metric name"
            placeholder="e.g., kubernetes.pod.cpu.usage"
            value={metadataForm.podCpuMetric}
            onChange={(e) => handleMetadataChange('podCpuMetric', e.target.value)}
          />
          <TextInput
            id="instana-pod-memory-metric"
            labelText="Pod memory metric name"
            placeholder="e.g., kubernetes.pod.memory.usage"
            value={metadataForm.podMemoryMetric}
            onChange={(e) => handleMetadataChange('podMemoryMetric', e.target.value)}
          />
          <TextInput
            id="instana-jvm-heap-metric"
            labelText="JVM heap used % metric"
            placeholder="e.g., jvm.memory.heap.used.percent"
            value={metadataForm.jvmHeapUsedMetric}
            onChange={(e) => handleMetadataChange('jvmHeapUsedMetric', e.target.value)}
          />
          <TextInput
            id="instana-jvm-gc-metric"
            labelText="JVM GC pause metric"
            placeholder="e.g., jvm.gc.pause.ms"
            value={metadataForm.jvmGcPauseMetric}
            onChange={(e) => handleMetadataChange('jvmGcPauseMetric', e.target.value)}
          />
          <TextInput
            id="instana-jvm-cpu-metric"
            labelText="JVM process CPU metric"
            placeholder="e.g., jvm.process.cpu.utilization"
            value={metadataForm.jvmProcessCpuMetric}
            onChange={(e) => handleMetadataChange('jvmProcessCpuMetric', e.target.value)}
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
                onClick={() => architecturePreviewDataUrl && window.open(architecturePreviewDataUrl, '_blank', 'noopener,noreferrer')}
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
                src={architecturePreviewDataUrl}
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

      <Tile className="capability-sla">
        <div className="capability-table-header">
          <h3>SLA Criteria (Acceptance Criteria)</h3>
          <p>Define performance acceptance criteria thresholds for pass/fail evaluation by test label.</p>
        </div>

        <div className="metadata-controls">
          <Select
            id="sla-label-select"
            labelText="Select test label"
            value={selectedSlaLabel}
            onChange={(e) => handleSlaLabelChange(e.target.value)}
            disabled={slaLabels.length === 0}
          >
            {slaLabels.length === 0 ? (
              <SelectItem value="" text="No labels defined" />
            ) : (
              slaLabels.map((label) => (
                <SelectItem key={label} value={label} text={label} />
              ))
            )}
          </Select>
          <Button
            kind="secondary"
            onClick={handleAddLabel}
            disabled={!selectedCapabilityId}
            size="sm"
          >
            Add Label
          </Button>
        </div>

        <div className="metadata-controls">
          <Button
            kind="primary"
            onClick={addSlaCriteria}
            disabled={!selectedCapabilityId || !selectedSlaLabel}
            renderIcon={Add}
            size="sm"
          >
            Add SLA Criterion
          </Button>
          <Button
            kind="primary"
            onClick={handleSaveSla}
            disabled={!selectedCapabilityId || slaSaving || slaForm.length === 0 || !selectedSlaLabel}
          >
            {slaSaving ? 'Saving...' : 'Save SLA criteria'}
          </Button>
        </div>

        {selectedSlaLabel ? (
          <>
            <p style={{ color: '#525252', marginTop: '1rem', marginBottom: '1rem' }}>
              <strong>Label:</strong> {selectedSlaLabel}
            </p>
            {slaForm.length === 0 ? (
              <p style={{ color: '#6f6f6f' }}>
                No criteria defined for this label. Click "Add SLA Criterion" to get started.
              </p>
            ) : (
              <div className="sla-criteria-list">
                {slaForm.map((criterion, idx) => (
                  <div key={idx} className="sla-criterion-row">
                    <TextInput
                      id={`sla-label-${idx}`}
                      labelText="Metric Name"
                      placeholder="e.g., Response Time, Error Rate"
                      value={criterion.label}
                      onChange={(e) => updateSlaCriteria(idx, 'label', e.target.value)}
                    />
                <Select
                  id={`sla-operator-${idx}`}
                  labelText="Operator"
                  value={criterion.operator}
                  onChange={(e) => updateSlaCriteria(idx, 'operator', e.target.value)}
                >
                  <SelectItem value="<" text="< (Less than)" />
                  <SelectItem value="<=" text="<= (Less than or equal)" />
                  <SelectItem value=">" text="> (Greater than)" />
                  <SelectItem value=">=" text=">= (Greater than or equal)" />
                  <SelectItem value="=" text="= (Equal)" />
                </Select>
                <TextInput
                  id={`sla-threshold-${idx}`}
                  labelText="Threshold Value"
                  placeholder="e.g., 500"
                  value={criterion.threshold}
                  onChange={(e) => updateSlaCriteria(idx, 'threshold', e.target.value)}
                />
                <Select
                  id={`sla-unit-${idx}`}
                  labelText="Unit"
                  value={criterion.unit}
                  onChange={(e) => updateSlaCriteria(idx, 'unit', e.target.value)}
                >
                  <SelectItem value="ms" text="ms (Milliseconds)" />
                  <SelectItem value="s" text="s (Seconds)" />
                  <SelectItem value="%" text="% (Percentage)" />
                  <SelectItem value="req/s" text="req/s (Requests per second)" />
                  <SelectItem value="count" text="count" />
                </Select>
                <Button
                  kind="danger--ghost"
                  size="sm"
                  renderIcon={TrashCan}
                  onClick={() => removeSlaCriteria(idx)}
                  iconDescription="Remove"
                  hasIconOnly
                />
              </div>
            ))}
              </div>
            )}
          </>
        ) : (
          <p style={{ color: '#6f6f6f', marginTop: '1rem' }}>
            Please select or add a test label to define SLA criteria.
          </p>
        )}
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
            <li>Capability metadata is required for report cover content.</li>
            <li>Baseline thresholds drive pass/fail evaluations in reports.</li>
            <li>Architecture diagrams can be reused across generated reports.</li>
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

