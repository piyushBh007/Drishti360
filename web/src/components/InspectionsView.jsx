import React, { useState } from 'react';

export default function InspectionsView({ 
  inspections, 
  ngos, 
  onScheduleInspection, 
  isModalOpen, 
  setIsModalOpen, 
  prefilledNgo 
}) {
  const [statusFilter, setStatusFilter] = useState('ALL');
  
  // Form State
  const [formData, setFormData] = useState({
    ngo_id: prefilledNgo?.ngo_id || prefilledNgo?.id || 'NGO-101',
    ngo_name: prefilledNgo?.name || 'Seva Rural Development Trust',
    inspector_name: 'Officer D. K. Sharma',
    scheduled_date: new Date().toISOString().split('T')[0],
    inspection_type: 'PHYSICAL_SURPRISE',
    notes: 'Triggered by AI Attendance Discrepancy (>60% discrepancy).',
    location: prefilledNgo?.district || 'South Delhi Center'
  });

  const filteredInspections = inspections.filter(insp => {
    if (statusFilter === 'ALL') return true;
    return insp.status === statusFilter;
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    onScheduleInspection(formData);
    setIsModalOpen(false);
  };

  const handleNgoSelect = (ngoId) => {
    const selected = ngos.find(n => n.id === ngoId || n.ngo_id === ngoId);
    if (selected) {
      setFormData(prev => ({
        ...prev,
        ngo_id: selected.ngo_id || selected.id,
        ngo_name: selected.name,
        location: `${selected.district || ''}, ${selected.state || ''}`
      }));
    }
  };

  return (
    <div>
      {/* Top Controls */}
      <div className="filter-bar">
        <div className="filter-group">
          <span style={{ fontSize: '0.8rem', fontWeight: 600, color: '#64748b' }}>Filter Status:</span>
          <button 
            className={`btn btn-sm ${statusFilter === 'ALL' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setStatusFilter('ALL')}
          >
            All ({inspections.length})
          </button>
          <button 
            className={`btn btn-sm ${statusFilter === 'SCHEDULED' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setStatusFilter('SCHEDULED')}
          >
            📅 Scheduled ({inspections.filter(i => i.status === 'SCHEDULED').length})
          </button>
          <button 
            className={`btn btn-sm ${statusFilter === 'IN_PROGRESS' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setStatusFilter('IN_PROGRESS')}
          >
            ⏳ In Progress ({inspections.filter(i => i.status === 'IN_PROGRESS').length})
          </button>
          <button 
            className={`btn btn-sm ${statusFilter === 'COMPLETED' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setStatusFilter('COMPLETED')}
          >
            ✅ Completed ({inspections.filter(i => i.status === 'COMPLETED').length})
          </button>
        </div>

        <button className="btn btn-primary" onClick={() => setIsModalOpen(true)}>
          ➕ Schedule New Inspection
        </button>
      </div>

      {/* Inspections Table */}
      <div className="card">
        <div className="card-header">
          <h3>📋 Field Inspection Schedule & Logs</h3>
        </div>
        <div className="card-body" style={{ padding: 0 }}>
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Inspection ID</th>
                  <th>Target NGO</th>
                  <th>Assigned Inspector</th>
                  <th>Scheduled Date</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Notes / AI Trigger</th>
                </tr>
              </thead>
              <tbody>
                {filteredInspections.map(insp => (
                  <tr key={insp.id}>
                    <td><code style={{ fontWeight: 700, color: '#2563eb' }}>{insp.inspection_id || insp.id}</code></td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{insp.ngo_name}</div>
                      <div style={{ fontSize: '0.75rem', color: '#64748b' }}>{insp.ngo_id}</div>
                    </td>
                    <td>{insp.inspector_name}</td>
                    <td>{insp.scheduled_date}</td>
                    <td>
                      <span className="badge badge-neutral" style={{ textTransform: 'capitalize' }}>
                        {insp.inspection_type ? insp.inspection_type.replace('_', ' ') : 'Surprise'}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${
                        insp.status === 'COMPLETED' ? 'badge-success' :
                        insp.status === 'IN_PROGRESS' ? 'badge-warning' : 'badge-info'
                      }`}>
                        {insp.status}
                      </span>
                    </td>
                    <td style={{ fontSize: '0.8rem', color: '#475569', maxWidth: '300px' }}>
                      {insp.notes}
                    </td>
                  </tr>
                ))}
                {filteredInspections.length === 0 && (
                  <tr>
                    <td colSpan="7" className="empty-state">No inspections found matching criteria.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Schedule Inspection Modal */}
      {isModalOpen && (
        <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>📋 Schedule Field Inspection</h3>
              <button className="modal-close" onClick={() => setIsModalOpen(false)}>&times;</button>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="modal-body">
                <div className="form-group">
                  <label>Target NGO</label>
                  <select 
                    className="form-control"
                    value={formData.ngo_id}
                    onChange={e => handleNgoSelect(e.target.value)}
                    required
                  >
                    {ngos.map(n => (
                      <option key={n.id} value={n.ngo_id || n.id}>
                        {n.name} ({n.risk_level || 'Risk'} Score: {n.risk_score})
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>Assigned Inspector Name</label>
                    <input 
                      type="text" 
                      className="form-control"
                      value={formData.inspector_name}
                      onChange={e => setFormData({ ...formData, inspector_name: e.target.value })}
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label>Scheduled Date</label>
                    <input 
                      type="date" 
                      className="form-control"
                      value={formData.scheduled_date}
                      onChange={e => setFormData({ ...formData, scheduled_date: e.target.value })}
                      required
                    />
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>Inspection Type</label>
                    <select 
                      className="form-control"
                      value={formData.inspection_type}
                      onChange={e => setFormData({ ...formData, inspection_type: e.target.value })}
                    >
                      <option value="PHYSICAL_SURPRISE">🚨 Surprise Physical Verification</option>
                      <option value="DIGITAL_AUDIT">💻 Digital Geofence & Log Audit</option>
                      <option value="ANNUAL_ROUTINE">📅 Routine Annual Inspection</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label>Location / Premises</label>
                    <input 
                      type="text" 
                      className="form-control"
                      value={formData.location}
                      onChange={e => setFormData({ ...formData, location: e.target.value })}
                      required
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label>Notes / Reason for Inspection</label>
                  <textarea 
                    className="form-control" 
                    rows="3"
                    value={formData.notes}
                    onChange={e => setFormData({ ...formData, notes: e.target.value })}
                    required
                  ></textarea>
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-outline" onClick={() => setIsModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Dispatch Inspection Task</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
