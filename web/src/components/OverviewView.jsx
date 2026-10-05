import React from 'react';

export default function OverviewView({ 
  overviewData, 
  ngos, 
  cameras, 
  alerts, 
  inspections, 
  onNavigate, 
  onSelectNgo, 
  onOpenNewInspection 
}) {
  const highRiskNgos = ngos.filter(n => n.risk_level === 'HIGH' || n.risk_score > 70);
  const activeAlerts = alerts.filter(a => a.status === 'ACTIVE');

  return (
    <div>
      {/* Banner */}
      <div className="judge-banner">
        <div className="judge-banner-content">
          <h2>🏛️ Drishti360 National Monitoring Center</h2>
          <p>
            Real-time compliance tracking across registered NGOs. Automated AI computer vision, GPS geofencing, and risk scoring flag anomalies before fund allocation.
          </p>
        </div>
        <div className="judge-banner-actions">
          <button className="btn btn-primary" onClick={onOpenNewInspection} style={{ background: '#ffffff', color: '#1e40af' }}>
            ➕ Schedule Surprise Inspection
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-info">
            <div className="stat-label">Total Registered NGOs</div>
            <div className="stat-value">{overviewData.totalNgos || ngos.length}</div>
            <div className="stat-subtext">Verified in National Registry</div>
          </div>
          <div className="stat-icon blue">🏢</div>
        </div>

        <div className="stat-card">
          <div className="stat-info">
            <div className="stat-label">High Risk Flagged</div>
            <div className="stat-value" style={{ color: '#dc2626' }}>{overviewData.highRiskNgos || highRiskNgos.length}</div>
            <div className="stat-subtext">XGBoost Score &gt; 70 Threshold</div>
          </div>
          <div className="stat-icon red">⚠️</div>
        </div>

        <div className="stat-card">
          <div className="stat-info">
            <div className="stat-label">Active AI Anomaly Alerts</div>
            <div className="stat-value" style={{ color: '#d97706' }}>{overviewData.activeAlerts || activeAlerts.length}</div>
            <div className="stat-subtext">Attendance / Geofence / Asset</div>
          </div>
          <div className="stat-icon amber">🔔</div>
        </div>

        <div className="stat-card">
          <div className="stat-info">
            <div className="stat-label">CCTV Feeds Streaming</div>
            <div className="stat-value" style={{ color: '#16a34a' }}>{overviewData.camerasOnline || cameras.length}</div>
            <div className="stat-subtext">YOLO v8 AI Active</div>
          </div>
          <div className="stat-icon green">📹</div>
        </div>
      </div>

      {/* Main Grid */}
      <div className="dashboard-grid">
        {/* Left Side: High Risk NGO Attention List */}
        <div className="card">
          <div className="card-header">
            <h3>🔴 High Risk NGO Monitoring List</h3>
            <button className="btn btn-outline btn-sm" onClick={() => onNavigate('ngos')}>View All NGOs</button>
          </div>
          <div className="card-body" style={{ padding: 0 }}>
            <div className="table-responsive">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>NGO ID & Name</th>
                    <th>State / District</th>
                    <th>Risk Index</th>
                    <th>Compliance</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {highRiskNgos.slice(0, 5).map(ngo => (
                    <tr key={ngo.id}>
                      <td>
                        <div style={{ fontWeight: 600 }}>{ngo.name}</div>
                        <div style={{ fontSize: '0.75rem', color: '#64748b' }}>{ngo.ngo_id || ngo.id} • Reg: {ngo.registration_no}</div>
                      </td>
                      <td>{ngo.state}, {ngo.district}</td>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          <span style={{ fontWeight: 700, color: '#dc2626' }}>{ngo.risk_score}</span>
                          <div className="risk-meter">
                            <div className="risk-meter-fill high" style={{ width: `${ngo.risk_score}%` }}></div>
                          </div>
                        </div>
                      </td>
                      <td>
                        <span className="badge badge-danger">{ngo.compliance_status || 'Flagged'}</span>
                      </td>
                      <td>
                        <button 
                          className="btn btn-secondary btn-sm"
                          onClick={() => onSelectNgo(ngo)}
                        >
                          Audit Risk
                        </button>
                      </td>
                    </tr>
                  ))}
                  {highRiskNgos.length === 0 && (
                    <tr>
                      <td colSpan="5" className="empty-state">No high-risk NGOs flagged.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* Right Side: Live Anomaly Alerts Feed */}
        <div className="card">
          <div className="card-header">
            <h3>🔔 Live Anomaly Feed</h3>
            <button className="btn btn-outline btn-sm" onClick={() => onNavigate('alerts')}>All Alerts</button>
          </div>
          <div className="card-body" style={{ padding: '0.75rem' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {alerts.slice(0, 4).map(alert => (
                <div 
                  key={alert.id}
                  style={{
                    padding: '0.75rem',
                    borderRadius: '8px',
                    border: '1px solid #fecaca',
                    background: alert.severity === 'CRITICAL' ? '#fef2f2' : '#fffbeb',
                    borderLeft: `4px solid ${alert.severity === 'CRITICAL' ? '#dc2626' : '#d97706'}`
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                    <span style={{ fontWeight: 600, fontSize: '0.8rem', color: '#0f172a' }}>{alert.ngo_name}</span>
                    <span className={`badge ${alert.severity === 'CRITICAL' ? 'badge-danger' : 'badge-warning'}`}>
                      {alert.severity}
                    </span>
                  </div>
                  <p style={{ fontSize: '0.775rem', color: '#334155', margin: '0 0 0.4rem 0' }}>
                    {alert.message}
                  </p>
                  <div style={{ fontSize: '0.7rem', color: '#94a3b8' }}>
                    🕒 {alert.timestamp}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Lower Section: CCTV Live AI Preview Strip */}
      <div className="card">
        <div className="card-header">
          <h3>📹 AI Camera Detection Monitoring</h3>
          <button className="btn btn-outline btn-sm" onClick={() => onNavigate('cctv')}>Open CCTV AI Console</button>
        </div>
        <div className="card-body">
          <div className="cctv-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))' }}>
            {cameras.slice(0, 3).map(cam => (
              <div key={cam.id} className="cctv-card">
                <div className="cctv-viewport">
                  <div className="cctv-canvas-mock">
                    <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>📹</div>
                    <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>{cam.name}</div>
                    <div className="cctv-overlay-tag">
                      <span className="status-dot"></span> LIVE 25 FPS
                    </div>
                    {cam.ai_status !== 'NORMAL' && (
                      <div className="cctv-ai-box" style={{ top: '35%', left: '15%', right: '15%', height: '40%' }}>
                        ⚠️ AI DETECTED: {cam.ai_status} ({cam.ai_confidence || 94}%)
                      </div>
                    )}
                  </div>
                </div>
                <div className="cctv-info">
                  <div className="cctv-info-header">
                    <div className="cctv-title">{cam.ngo_name}</div>
                    <span className={`badge ${cam.ai_status === 'NORMAL' ? 'badge-success' : 'badge-danger'}`}>
                      {cam.ai_status}
                    </span>
                  </div>
                  <div className="cctv-location">📍 {cam.location}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
