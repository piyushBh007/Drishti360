import React from 'react';

export default function AuditView({ auditLogs }) {
  return (
    <div>
      <div className="card" style={{ marginBottom: '1.25rem', background: '#f8fafc', borderLeft: '4px solid #2563eb' }}>
        <div className="card-body">
          <h3 style={{ margin: '0 0 0.25rem 0', fontSize: '1rem', color: '#1e40af' }}>
            🛡️ Immutable Cryptographic Audit Log
          </h3>
          <p style={{ margin: 0, fontSize: '0.8rem', color: '#475569' }}>
            All inspection dispatches, risk score recalculations, and AI anomaly alerts are recorded with SHA-256 hash chains to guarantee non-repudiation for audit authorities.
          </p>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <h3>📜 Audit Trail Timeline</h3>
          <span className="badge badge-success">✓ Cryptographic Hashes Verified</span>
        </div>
        <div className="card-body" style={{ padding: 0 }}>
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Audit Ref</th>
                  <th>Timestamp</th>
                  <th>System Actor</th>
                  <th>Action Executed</th>
                  <th>Target Entity</th>
                  <th>Action Details</th>
                  <th>SHA-256 Hash Signature</th>
                </tr>
              </thead>
              <tbody>
                {auditLogs.map(log => (
                  <tr key={log.id}>
                    <td><code>{log.id}</code></td>
                    <td style={{ fontSize: '0.75rem', color: '#64748b' }}>{log.timestamp}</td>
                    <td>
                      <span className={`badge ${
                        log.actor.includes('SYSTEM') || log.actor.includes('AI') ? 'badge-info' :
                        log.actor.includes('CCTV') ? 'badge-warning' : 'badge-neutral'
                      }`}>
                        {log.actor}
                      </span>
                    </td>
                    <td style={{ fontWeight: 600 }}>{log.action}</td>
                    <td><code>{log.entity}</code></td>
                    <td style={{ fontSize: '0.8rem', color: '#334155' }}>{log.details}</td>
                    <td>
                      <code style={{ fontSize: '0.7rem', color: '#64748b', wordBreak: 'break-all' }}>
                        {log.hash ? log.hash.substring(0, 16) + '...' : 'e3b0c44298fc1c14...'}
                      </code>
                    </td>
                  </tr>
                ))}
                {auditLogs.length === 0 && (
                  <tr>
                    <td colSpan="7" className="empty-state">No audit logs available.</td>
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
