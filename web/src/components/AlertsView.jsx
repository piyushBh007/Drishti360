import React, { useState } from 'react';

export default function AlertsView({ alerts, onResolveAlert }) {
  const [filter, setFilter] = useState('ALL');

  const filteredAlerts = alerts.filter(a => {
    if (filter === 'ALL') return true;
    if (filter === 'ACTIVE') return a.status === 'ACTIVE';
    if (filter === 'CRITICAL') return a.severity === 'CRITICAL';
    if (filter === 'RESOLVED') return a.status === 'RESOLVED';
    return true;
  });

  return (
    <div>
      <div className="filter-bar">
        <div className="filter-group">
          <span style={{ fontSize: '0.8rem', fontWeight: 600, color: '#64748b' }}>Filter Alerts:</span>
          <button 
            className={`btn btn-sm ${filter === 'ALL' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setFilter('ALL')}
          >
            All ({alerts.length})
          </button>
          <button 
            className={`btn btn-sm ${filter === 'ACTIVE' ? 'btn-danger' : 'btn-outline'}`}
            onClick={() => setFilter('ACTIVE')}
          >
            🚨 Active ({alerts.filter(a => a.status === 'ACTIVE').length})
          </button>
          <button 
            className={`btn btn-sm ${filter === 'CRITICAL' ? 'btn-secondary' : 'btn-outline'}`}
            onClick={() => setFilter('CRITICAL')}
          >
            🔴 Critical ({alerts.filter(a => a.severity === 'CRITICAL').length})
          </button>
          <button 
            className={`btn btn-sm ${filter === 'RESOLVED' ? 'btn-secondary' : 'btn-outline'}`}
            onClick={() => setFilter('RESOLVED')}
          >
            ✅ Resolved ({alerts.filter(a => a.status === 'RESOLVED').length})
          </button>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <h3>🔔 AI Anomaly & Compliance Alerts Log</h3>
        </div>
        <div className="card-body" style={{ padding: 0 }}>
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Alert ID</th>
                  <th>Target NGO</th>
                  <th>Severity</th>
                  <th>Anomaly Type</th>
                  <th>Message / Trigger Details</th>
                  <th>Timestamp</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {filteredAlerts.map(alert => (
                  <tr key={alert.id} style={{ background: alert.status === 'RESOLVED' ? '#f8fafc' : undefined }}>
                    <td><code>{alert.alert_id || alert.id}</code></td>
                    <td style={{ fontWeight: 600 }}>{alert.ngo_name}</td>
                    <td>
                      <span className={`badge ${
                        alert.severity === 'CRITICAL' ? 'badge-danger' :
                        alert.severity === 'WARNING' ? 'badge-warning' : 'badge-info'
                      }`}>
                        {alert.severity}
                      </span>
                    </td>
                    <td>
                      <span className="badge badge-neutral">
                        {alert.type ? alert.type.replace(/_/g, ' ') : 'AI Anomaly'}
                      </span>
                    </td>
                    <td style={{ maxWidth: '350px', fontSize: '0.8rem' }}>
                      {alert.message}
                    </td>
                    <td style={{ fontSize: '0.75rem', color: '#64748b' }}>
                      {alert.timestamp}
                    </td>
                    <td>
                      {alert.status === 'ACTIVE' ? (
                        <button 
                          className="btn btn-outline btn-sm"
                          onClick={() => onResolveAlert(alert.id || alert.alert_id)}
                        >
                          ✓ Acknowledge
                        </button>
                      ) : (
                        <span style={{ fontSize: '0.75rem', color: '#16a34a', fontWeight: 600 }}>
                          ✅ Resolved
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
                {filteredAlerts.length === 0 && (
                  <tr>
                    <td colSpan="7" className="empty-state">No alerts found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
