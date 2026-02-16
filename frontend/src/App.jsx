import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import FileUpload from './pages/FileUpload';
import Reports from './pages/Reports';
import UploadedFiles from './pages/UploadedFiles';
import CapabilityDetails from './pages/CapabilityDetails';
import ReportBuilder from './pages/ReportBuilder';
import MainLayout from './components/Layout/MainLayout';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="upload" element={<FileUpload />} />
          <Route path="uploaded-files" element={<UploadedFiles />} />
          <Route path="reports" element={<Reports />} />
          <Route path="capability-details" element={<CapabilityDetails />} />
          <Route path="report-builder/:testRunId" element={<ReportBuilder />} />
        </Route>
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Router>
  );
}

export default App;

// Made with Bob
