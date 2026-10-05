import React, { useState } from 'react';

export default function NgosView({ ngos, selectedNgo, setSelectedNgo, onOpenNewInspection }) {
  const [searchTerm, setSearchTerm] = useState('');
  const [riskFilter, setRiskFilter] = useState('ALL');

  const filteredNgos = ngos.filter(ngo => {
    const matchesSearch = 
      ngo.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (ngo.state && ngo.state.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (ngo.registration_no && ngo.registration_no.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (ngo.ngo_id && ngo.ngo_id.toLowerCase().includes(searchTerm.toLowerCase()));

    if (!matchesSearch) return false;

    if (riskFilter === 'HIGH') return ngo.risk_level === 'HIGH' || ngo.risk_score > 70;
    if (riskFilter === 'MEDIUM') return ngo.risk_level === 'MEDIUM' || (ngo.risk_score >= 40 && ngo.risk_score <= 70);
    if (riskFilter === 'LOW') return ngo.risk_level === 'LOW' || ngo.risk_score < 40;

    return true;
  });

  return (
    <div>
      {/* Filter Bar */}
      <div className="filter-bar">
        <div className="filter-group" style={{ flex: 1, maxWidth: '400px' }}>
          <input
            type="text"
            className="form-control"
            placeholder="🔍 Search NGO by name, state, registration no..."
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
          />
        </div>

        <div className="filter-group">
          <span style={{ fontSize: '0.8rem', fontWeight: 600, color: '#64748b' }}>Risk Filter:</span>
          <button 
            className={`btn btn-sm ${riskFilter === 'ALL' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setRiskFilter('ALL')}
          >
            All ({ngos.length})
          </button>
          <button 
            className={`btn btn-sm ${riskFilter === 'HIGH' ? 'btn-danger' : 'btn-outline'}`}
            onClick={() => setRiskFilter('HIGH')}
          >
            🔴 High Risk ({ngos.filter(n => n.risk_level === 'HIGH' || n.risk_score > 70).length})
          </button>
          <button 
            className={`btn btn-sm ${riskFilter === 'MEDIUM' ? 'btn-secondary' : 'btn-outline'}`}
            onClick={() => setRiskFilter('MEDIUM')}
          >
            🟡 Medium ({ngos.filter(n => n.risk_level === 'MEDIUM' || (n.risk_score >= 40 && n.risk_score <= 70)).length})
          </button>
          <button 
            className={`btn btn-sm ${riskFilter === 'LOW' ? 'btn-secondary' : 'btn-outline'}`}
            onClick={() => setRiskFilter('LOW')}
          >
            🟢 Low Risk ({ngos.filter(n => n.risk_level === 'LOW' || n.risk_score < 40).length})
          </button>
        </div>
      </div>

      {/* Main Table */}
      <div className="card">
        <div className="card-header">
          <h3>🏢 Registered NGO Directory ({filteredNgos.length} Listed)</h3>
        </div>
        <div className="card-body" style={{ padding: 0 }}>
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>NGO Code & Name</th>
                  <th>Reg No</th>
                  <th>Location</th>
                  <th>XGBoost Risk Score</th>
                  <th>Status</th>
                  <th>CCTV & Inspections</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {filteredNgos.map(ngo => (
                  <tr key={ngo.id}>
                    <td>
                      <div style={{ fontWeight: 600, color: '#0f172a' }}>{ngo.name}</div>
                      <div style={{ fontSize: '0.75rem', color: '#64748b' }}>ID: {ngo.ngo_id || ngo.id}</div>
                    </td>
                    <td><code style={{ fontSize: '0.75rem' }}>{ngo.registration_no}</code></td>
                    <td>{ngo.state}, {ngo.district}</td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <span style={{ 
                          fontWeight: 700,
                          color: ngo.risk_score > 70 ? '#dc2626' : ngo.risk_score > 40 ? '#d97706' : '#16a34a' 
                        }}>
                          {ngo.risk_score}
                        </span>
                        <div className="risk-meter">
                          <div 
                            className={`risk-meter-fill ${
                              ngo.risk_score > 70 ? 'high' : ngo.risk_score > 40 ? 'medium' : 'low'
                            }`} 
                            style={{ width: `${ngo.risk_score}%` }}
                          ></div>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span className={`badge ${
                        ngo.risk_score > 70 ? 'badge-danger' : ngo.risk_score > 40 ? 'badge-warning' : 'badge-success'
                      }`}>
                        {ngo.compliance_status || (ngo.risk_score > 70 ? 'Flagged' : 'Compliant')}
                      </span>
                    </td>
                    <td>
                      <div style={{ fontSize: '0.75rem', color: '#475569' }}>
                        📹 {ngo.cameras_active || 1} Feeds • 📋 {ngo.active_inspections || 0} Pending
                      </div>
                    </td>
                    <td>
                      <button 
                        className="btn btn-outline btn-sm"
                        onClick={() => setSelectedNgo(ngo)}
                      >
                        🔎 View Risk Profile
                      </button>
                    </td>
                  </tr>
                ))}
                {filteredNgos.length === 0 && (
                  <tr>
                    <td colSpan="7" className="empty-state">No matching NGOs found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* NGO Detail Modal */}
      {selectedNgo && (
        <div className="modal-overlay" onClick={() => setSelectedNgo(null)}>
          <div className="modal-content" style={{ maxWidth: '700px' }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <h3 style={{ margin: 0 }}>🏢 {selectedNgo.name}</h3>
                <span style={{ fontSize: '0.75rem', color: '#64748b' }}>
                  {selectedNgo.ngo_id || selectedNgo.id} • Reg: {selectedNgo.registration_no}
                </span>
              </div>
              <button className="modal-close" onClick={() => setSelectedNgo(null)}>&times;</button>
            </div>

            <div className="modal-body">
              {/* Top Summary Card */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.25rem' }}>
                <div style={{ padding: '1rem', background: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                  <div style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 600 }}>CONTACT & LOCATION</div>
                  <div style={{ fontWeight: 600, margin: '0.25rem 0' }}>{selectedNgo.contact_person || 'N/A'}</div>
                  <div style={{ fontSize: '0.8rem', color: '#475569' }}>📍 {selectedNgo.district}, {selectedNgo.state}</div>
                  <div style={{ fontSize: '0.8rem', color: '#475569' }}>📞 {selectedNgo.phone || '+91 98000 00000'}</div>
                  {selectedNgo.latitude && (
                    <div style={{ fontSize: '0.75rem', color: '#2563eb', marginTop: '0.25rem' }}>
                      🌐 GPS: {selectedNgo.latitude}, {selectedNgo.longitude}
                    </div>
                  )}
                </div>

                <div style={{ padding: '1rem', background: selectedNgo.risk_score > 70 ? '#fef2f2' : '#ecfdf5', borderRadius: '8px', border: '1px solid #cbd5e1' }}>
                  <div style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 600 }}>XGBOOST RISK EVALUATION</div>
                  <div style={{ fontSize: '2rem', fontWeight: 700, color: selectedNgo.risk_score > 70 ? '#dc2626' : '#059669' }}>
                    {selectedNgo.risk_score} / 100
                  </div>
                  <span className={`badge ${selectedNgo.risk_score > 70 ? 'badge-danger' : 'badge-success'}`}>
                    Risk Tier: {selectedNgo.risk_level || 'HIGH'}
                  </span>
                </div>
              </div>

              {/* Risk Factor Breakdown */}
              <h4 style={{ marginBottom: '0.5rem', fontSize: '0.9rem' }}>📊 Risk Component Breakdown</h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '1.25rem' }}>
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: '0.2rem' }}>
                    <span>CCTV Attendance Discrepancy</span>
                    <span style={{ fontWeight: 600 }}>{selectedNgo.risk_breakdown?.cctv_anomalies || 38}%</span>
                  </div>
                  <div className="risk-meter" style={{ width: '100%', height: '6px' }}>
                    <div className="risk-meter-fill high" style={{ width: `${selectedNgo.risk_breakdown?.cctv_anomalies || 38}%` }}></div>
                  </div>
                </div>

                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: '0.2rem' }}>
                    <span>GPS Spoofing & Geofence Drift Alerts</span>
                    <span style={{ fontWeight: 600 }}>{selectedNgo.risk_breakdown?.gps_spoof_alerts || 22}%</span>
                  </div>
                  <div className="risk-meter" style={{ width: '100%', height: '6px' }}>
                    <div className="risk-meter-fill medium" style={{ width: `${selectedNgo.risk_breakdown?.gps_spoof_alerts || 22}%` }}></div>
                  </div>
                </div>

                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: '0.2rem' }}>
                    <span>Fund Utilization Lag</span>
                    <span style={{ fontWeight: 600 }}>{selectedNgo.risk_breakdown?.fund_utilization_lag || 24.5}%</span>
                  </div>
                  <div className="risk-meter" style={{ width: '100%', height: '6px' }}>
                    <div className="risk-meter-fill medium" style={{ width: `${selectedNgo.risk_breakdown?.fund_utilization_lag || 24.5}%` }}></div>
                  </div>
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn btn-outline" onClick={() => setSelectedNgo(null)}>Close</button>
              <button 
                className="btn btn-primary"
                onClick={() => {
                  const ngoToInspect = selectedNgo;
                  setSelectedNgo(null);
                  onOpenNewInspection(ngoToInspect);
                }}
              >
                📋 Schedule Surprise Inspection
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
