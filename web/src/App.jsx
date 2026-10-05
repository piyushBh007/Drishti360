import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar.jsx';
import JudgePitchModal from './components/JudgePitchModal.jsx';
import OverviewView from './components/OverviewView.jsx';
import NgosView from './components/NgosView.jsx';
import CctvView from './components/CctvView.jsx';
import InspectionsView from './components/InspectionsView.jsx';
import AlertsView from './components/AlertsView.jsx';
import AuditView from './components/AuditView.jsx';
import { api } from './api/client.js';

export default function App() {
  const [activeTab, setActiveTab] = useState('overview');
  const [backendStatus, setBackendStatus] = useState(true);
  const [isPitchModalOpen, setIsPitchModalOpen] = useState(false);
  
  // Data States
  const [overviewData, setOverviewData] = useState({});
  const [ngos, setNgos] = useState([]);
  const [cameras, setCameras] = useState([]);
  const [inspections, setInspections] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  // Selected NGO for detail modal
  const [selectedNgo, setSelectedNgo] = useState(null);

  // Schedule Inspection Modal State
  const [isInspectionModalOpen, setIsInspectionModalOpen] = useState(false);
  const [prefilledNgo, setPrefilledNgo] = useState(null);

  // Initial Data Fetch
  useEffect(() => {
    async function loadDashboardData() {
      setLoading(true);
      
      // Check health
      const health = await api.checkBackendHealth();
      setBackendStatus(health.online);

      // Load all data concurrently
      const [ov, ngosList, camsList, inspList, alertsList, auditList] = await Promise.all([
        api.getOverview(),
        api.getNgos(),
        api.getCameras(),
        api.getInspections(),
        api.getAlerts(),
        api.getAuditLogs()
      ]);

      setOverviewData(ov);
      setNgos(ngosList);
      setCameras(camsList);
      setInspections(inspList);
      setAlerts(alertsList);
      setAuditLogs(auditList);
      setLoading(false);
    }

    loadDashboardData();
  }, []);

  // Handler to open Schedule Inspection Modal from any screen
  const handleOpenNewInspection = (ngoToPrefill = null) => {
    setPrefilledNgo(ngoToPrefill);
    setIsInspectionModalOpen(true);
  };

  // Handler to schedule inspection
  const handleScheduleInspection = async (formData) => {
    const newInsp = await api.createInspection(formData);
    setInspections(prev => [newInsp, ...prev]);
    setActiveTab('inspections');
  };

  // Handler to resolve alert
  const handleResolveAlert = async (alertId) => {
    await api.resolveAlert(alertId);
    setAlerts(prev => prev.map(a => (a.id === alertId || a.alert_id === alertId) ? { ...a, status: 'RESOLVED' } : a));
  };

  return (
    <div className="app-container">
      <Navbar 
        activeTab={activeTab} 
        setActiveTab={setActiveTab} 
        backendStatus={backendStatus}
        onOpenPitchModal={() => setIsPitchModalOpen(true)}
      />

      <main className="main-wrapper">
        <div className="main-content">
          {loading ? (
            <div style={{ padding: '4rem', textAlign: 'center', color: '#64748b' }}>
              <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>⚙️</div>
              <h3 style={{ margin: 0 }}>Connecting to Drishti360 Governance Portal...</h3>
              <p style={{ fontSize: '0.85rem' }}>Fetching live data from Render backend</p>
            </div>
          ) : (
            <>
              {activeTab === 'overview' && (
                <OverviewView 
                  overviewData={overviewData}
                  ngos={ngos}
                  cameras={cameras}
                  alerts={alerts}
                  inspections={inspections}
                  onNavigate={setActiveTab}
                  onSelectNgo={setSelectedNgo}
                  onOpenNewInspection={() => handleOpenNewInspection()}
                />
              )}

              {activeTab === 'ngos' && (
                <NgosView 
                  ngos={ngos}
                  selectedNgo={selectedNgo}
                  setSelectedNgo={setSelectedNgo}
                  onOpenNewInspection={handleOpenNewInspection}
                />
              )}

              {activeTab === 'cctv' && (
                <CctvView 
                  cameras={cameras}
                  onOpenNewInspection={handleOpenNewInspection}
                />
              )}

              {activeTab === 'inspections' && (
                <InspectionsView 
                  inspections={inspections}
                  ngos={ngos}
                  onScheduleInspection={handleScheduleInspection}
                  isModalOpen={isInspectionModalOpen}
                  setIsModalOpen={setIsInspectionModalOpen}
                  prefilledNgo={prefilledNgo}
                />
              )}

              {activeTab === 'alerts' && (
                <AlertsView 
                  alerts={alerts}
                  onResolveAlert={handleResolveAlert}
                />
              )}

              {activeTab === 'audit' && (
                <AuditView 
                  auditLogs={auditLogs}
                />
              )}
            </>
          )}
        </div>
      </main>

      <footer className="footer">
        <div style={{ maxWidth: '1200px', margin: '0 auto', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <strong>DRISHTI 360</strong> — National NGO Monitoring & Fraud Prevention Platform
          </div>
          <div>
            Backend: <a href="https://drishti360-backend.onrender.com" target="_blank" rel="noreferrer">Render Production Service</a>
          </div>
        </div>
      </footer>

      {/* Judge Pitch & Architecture Info Modal */}
      <JudgePitchModal 
        isOpen={isPitchModalOpen} 
        onClose={() => setIsPitchModalOpen(false)} 
      />
    </div>
  );
}
