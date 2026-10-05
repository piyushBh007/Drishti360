import React from 'react';

export default function Navbar({ activeTab, setActiveTab, backendStatus, onOpenPitchModal }) {
  return (
    <header className="top-nav">
      <div className="brand">
        <div className="brand-logo">D360</div>
        <div className="brand-text">
          <h1>DRISHTI 360</h1>
          <p>National NGO Governance & AI Compliance Portal</p>
        </div>
      </div>

      <nav className="nav-links">
        <button
          className={`nav-item ${activeTab === 'overview' ? 'active' : ''}`}
          onClick={() => setActiveTab('overview')}
        >
          📊 Overview
        </button>
        <button
          className={`nav-item ${activeTab === 'ngos' ? 'active' : ''}`}
          onClick={() => setActiveTab('ngos')}
        >
          🏢 NGOs & Risk
        </button>
        <button
          className={`nav-item ${activeTab === 'cctv' ? 'active' : ''}`}
          onClick={() => setActiveTab('cctv')}
        >
          📹 CCTV AI Feeds
        </button>
        <button
          className={`nav-item ${activeTab === 'inspections' ? 'active' : ''}`}
          onClick={() => setActiveTab('inspections')}
        >
          📋 Inspections
        </button>
        <button
          className={`nav-item ${activeTab === 'alerts' ? 'active' : ''}`}
          onClick={() => setActiveTab('alerts')}
        >
          🔔 Alerts
        </button>
        <button
          className={`nav-item ${activeTab === 'audit' ? 'active' : ''}`}
          onClick={() => setActiveTab('audit')}
        >
          🛡️ Audit Trail
        </button>
      </nav>

      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <button 
          className="btn btn-primary btn-sm"
          onClick={onOpenPitchModal}
          style={{ background: '#2563eb', border: 'none', color: '#fff' }}
        >
          💡 Judge Info & Arch
        </button>

        <div className="backend-status">
          <span className={`status-dot ${backendStatus ? '' : 'offline'}`}></span>
          <span>{backendStatus ? 'Live Backend Connected' : 'Render Cold-Start / Offline'}</span>
        </div>
      </div>
    </header>
  );
}
