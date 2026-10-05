import React, { useState } from 'react';

export default function CctvView({ cameras, onOpenNewInspection }) {
  const [filterNgo, setFilterNgo] = useState('ALL');

  const filteredCameras = cameras.filter(cam => {
    if (filterNgo === 'ALL') return true;
    return cam.ngo_name.toLowerCase().includes(filterNgo.toLowerCase());
  });

  return (
    <div>
      {/* Header info */}
      <div className="card" style={{ marginBottom: '1.25rem', background: '#0f172a', color: 'white' }}>
        <div className="card-body" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h3 style={{ margin: '0 0 0.25rem 0', color: 'white', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              📹 YOLO v8 Edge AI Vision Console
            </h3>
            <p style={{ margin: 0, fontSize: '0.8rem', color: '#94a3b8' }}>
              Continuous real-time stream analysis detecting classroom beneficiary attendance discrepancies & asset inventory anomalies.
            </p>
          </div>
          <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
            <span className="badge badge-success" style={{ padding: '0.4rem 0.75rem', fontSize: '0.8rem' }}>
              ● 8 Streams Connected
            </span>
          </div>
        </div>
      </div>

      {/* Grid */}
      <div className="cctv-grid">
        {filteredCameras.map(cam => (
          <div key={cam.id} className="cctv-card">
            <div className="cctv-viewport">
              <div className="cctv-canvas-mock">
                {/* Simulated video frame graphics */}
                <div style={{ position: 'absolute', inset: 0, opacity: 0.15, background: 'linear-gradient(45deg, #3b82f6 25%, transparent 25%), linear-gradient(-45deg, #3b82f6 25%, transparent 25%), linear-gradient(45deg, transparent 75%, #3b82f6 75%), linear-gradient(-45deg, transparent 75%, #3b82f6 75%)', backgroundSize: '20px 20px' }}></div>

                <div style={{ fontSize: '2.5rem', zIndex: 1, filter: 'drop-shadow(0 0 8px rgba(37,99,235,0.5))' }}>
                  📹
                </div>
                <div style={{ fontSize: '0.75rem', color: '#cbd5e1', zIndex: 1, marginTop: '0.25rem' }}>
                  {cam.name}
                </div>

                <div className="cctv-overlay-tag">
                  <span className="status-dot"></span> 1080p • 25 FPS • AI ON
                </div>

                {cam.ai_status !== 'NORMAL' ? (
                  <div 
                    className="cctv-ai-box" 
                    style={{ 
                      top: '25%', 
                      left: '10%', 
                      right: '10%', 
                      bottom: '25%',
                      border: '2px solid #ef4444',
                      background: 'rgba(239, 68, 68, 0.2)'
                    }}
                  >
                    <span style={{ background: '#dc2626', color: 'white', padding: '2px 6px', borderRadius: '2px', fontSize: '0.65rem' }}>
                      ⚠️ AI ANOMALY DETECTED ({cam.ai_confidence || 94.2}%)
                    </span>
                    <div style={{ fontSize: '0.7rem', color: '#fef08a', marginTop: '0.5rem', fontWeight: 600 }}>
                      {cam.bounding_boxes?.[0]?.label || 'Headcount Discrepancy: Claimed 45 vs Detected 12'}
                    </div>
                  </div>
                ) : (
                  <div style={{ position: 'absolute', bottom: 10, right: 10, background: 'rgba(34,197,94,0.2)', border: '1px solid #22c55e', color: '#4ade80', fontSize: '0.65rem', padding: '2px 6px', borderRadius: '4px' }}>
                    ✓ Normal Classroom Count (38 Present)
                  </div>
                )}
              </div>
            </div>

            <div className="cctv-info">
              <div className="cctv-info-header">
                <div>
                  <div className="cctv-title">{cam.ngo_name}</div>
                  <div className="cctv-location">📍 {cam.location}</div>
                </div>
                <span className={`badge ${cam.ai_status === 'NORMAL' ? 'badge-success' : 'badge-danger'}`}>
                  {cam.ai_status}
                </span>
              </div>

              <div style={{ marginTop: '0.5rem', paddingTop: '0.5rem', borderTop: '1px solid #1e293b', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.7rem', color: '#64748b' }}>Last verified: {cam.last_frame_time || 'Just now'}</span>
                {cam.ai_status !== 'NORMAL' && (
                  <button 
                    className="btn btn-danger btn-sm"
                    onClick={() => onOpenNewInspection({ name: cam.ngo_name, ngo_id: cam.ngo_id || 'NGO-101', district: cam.location })}
                  >
                    🚨 Trigger Surprise Inspection
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
