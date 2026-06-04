import { useState, useEffect } from 'react';

const cache = {};

export function useCaseDetails() {
  const [details, setDetails] = useState({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (Object.keys(cache).length > 0) { setDetails(cache); return; }
    setLoading(true);
    fetch('/api/test/case-details')
      .then(r => r.json())
      .then(data => {
        const map = {};
        for (const d of (Array.isArray(data) ? data : [])) {
          const key = d.className || d.module || 'other';
          if (!map[key]) map[key] = [];
          map[key].push(d);
        }
        Object.assign(cache, map);
        setDetails(map);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  // Lookup by className, or by caseId pattern
  const getByClassName = (name) => details[name] || [];
  const getByCaseId = (id) => {
    for (const arr of Object.values(details)) {
      const found = arr.find(d => d.caseId === id);
      if (found) return found;
    }
    return null;
  };

  return { details, loading, getByClassName, getByCaseId };
}
