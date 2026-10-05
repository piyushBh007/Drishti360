import React from 'react';

export default function JudgePitchModal({ isOpen, onClose }) {
  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" style={{ maxWidth: '750px' }} onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h3 style={{ margin: 0, color: '#0f172a' }}>🏛️ Drishti360 Architecture & Judge Guide</h3>
            <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Project Overview for Hackathon Evaluation</span>
          </div>
          <button className="modal-close" onClick={onClose}>&times;</button>
        </div>

        <div className="modal-body" style={{ fontSize: '0.875rem', color: '#334155', lineHeight: 1.6 }}>
          <div style={{ background: '#f0f9ff', border: '1px solid #bae6fd', padding: '1rem', borderRadius: '8px', marginBottom: '1.25rem' }}>
            <h4 style={{ color: '#0369a1', margin: '0 0 0.5rem 0', fontSize: '0.95rem' }}>🎯 The Core Problem Drishti360 Solves</h4>
            <p style={{ margin: 0 }}>
              Over ₹15,000 Cr in government subsidies are allocated to NGOs annually. Drishti360 prevents <strong>ghost beneficiaries, fund diversion, and spoofed geotags</strong> through automated multi-layered AI verification and immutable audit logs.
            </p>
          </div>

          <h4 style={{ color: '#0f172a', marginBottom: '0.75rem' }}>⚡ System Architecture Components</h4>
          
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.25rem' }}>
            <div style={{ border: '1px solid #e2e8f0', padding: '0.85rem', borderRadius: '8px', background: '#fafafa' }}>
              <div style={{ fontWeight: 600, color: '#1e40af', marginBottom: '0.25rem' }}>📱 1. Android Field Inspector App</div>
              <p style={{ fontSize: '0.8rem', color: '#64748b', margin: 0 }}>
                Native Android app with real-time GPS geotagging, surprise inspection checklist, WebRTC live video streaming, and offline sync.
              </p>
            </div>

            <div style={{ border: '1px solid #e2e8f0', padding: '0.85rem', borderRadius: '8px', background: '#fafafa' }}>
              <div style={{ fontWeight: 600, color: '#1e40af', marginBottom: '0.25rem' }}>📹 2. Edge CCTV & YOLO v8 AI</div>
              <p style={{ fontSize: '0.8rem', color: '#64748b', margin: 0 }}>
                Continuous video stream analysis detecting beneficiary attendance anomalies, classroom headcounts, and asset presence.
              </p>
            </div>

            <div style={{ border: '1px solid #e2e8f0', padding: '0.85rem', borderRadius: '8px', background: '#fafafa' }}>
              <div style={{ fontWeight: 600, color: '#1e40af', marginBottom: '0.25rem' }}>🧠 3. XGBoost Risk & PostGIS Engine</div>
              <p style={{ fontSize: '0.8rem', color: '#64748b', margin: 0 }}>
                Calculates composite risk scores (0–100) using historical compliance, spatial geofence drift, and CCTV detection deltas.
              </p>
            </div>

            <div style={{ border: '1px solid #e2e8f0', padding: '0.85rem', borderRadius: '8px', background: '#fafafa' }}>
              <div style={{ fontWeight: 600, color: '#1e40af', marginBottom: '0.25rem' }}>🖥️ 4. Official Web Governance Portal</div>
              <p style={{ fontSize: '0.8rem', color: '#64748b', margin: 0 }}>
                Fast, responsive web dashboard (this portal) connected to the production backend on Render with PostgreSQL / PostGIS.
              </p>
            </div>
          </div>

          <h4 style={{ color: '#0f172a', marginBottom: '0.5rem' }}>🔗 Backend Endpoint Verification</h4>
          <p style={{ fontSize: '0.8rem', color: '#64748b', marginBottom: '1rem' }}>
            Production API Base URL: <code>https://drishti360-backend.onrender.com</code><br/>
            Connected endpoints: <code>/api/overview</code>, <code>/api/ngos</code>, <code>/api/cameras</code>, <code>/api/inspections</code>, <code>/api/alerts</code>, <code>/api/audit-logs</code>.
          </p>
        </div>

        <div className="modal-footer">
          <button className="btn btn-primary" onClick={onClose}>
            Got it, Explore Dashboard
          </button>
        </div>
      </div>
    </div>
  );
}
