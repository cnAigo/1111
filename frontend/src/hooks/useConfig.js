import { useState, useCallback } from 'react';

export function useConfig(showToast) {
  const [configOpen, setConfigOpen] = useState(false);
  const [cfgUrl, setCfgUrl] = useState('https://192.168.6.171:8088');
  const [cfgProjectId, setCfgProjectId] = useState('2058851105448046592');
  const [cfgUsername, setCfgUsername] = useState('admin');
  const [cfgPassword, setCfgPassword] = useState('Aa123456');
  const [pwVisible, setPwVisible] = useState(false);
  const [savedConfigs, setSavedConfigs] = useState([]);
  const [configFormName, setConfigFormName] = useState('');

  const apiPost = async (url, body) => {
    const r = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    return r.json();
  };
  const apiGet = async (url) => { const r = await fetch(url); return r.json(); };
  const apiDelete = async (url) => { const r = await fetch(url, { method: 'DELETE' }); return r.json(); };

  const loadConfigs = useCallback(async () => {
    try { const d = await apiGet('/api/test/configs'); if (Array.isArray(d)) setSavedConfigs(d); }
    catch (e) { showToast('加载配置失败: ' + e.message, 'error'); }
  }, [showToast]);

  const saveConfig = useCallback(async () => {
    if (!configFormName.trim()) { showToast('请输入配置名称', 'warning'); return; }
    try { await apiPost('/api/test/configs', { configName: configFormName, url: cfgUrl, projectId: cfgProjectId, username: cfgUsername, password: cfgPassword }); showToast('已保存', 'success'); setConfigFormName(''); loadConfigs(); }
    catch (e) { showToast('保存失败: ' + e.message, 'error'); }
  }, [configFormName, cfgUrl, cfgProjectId, cfgUsername, cfgPassword, showToast, loadConfigs]);

  const deleteConfig = useCallback(async (id) => {
    try { await apiDelete(`/api/test/configs/${id}`); setSavedConfigs(p => p.filter(c => c.id !== id)); showToast('已删除', 'info'); }
    catch (e) { showToast('删除失败: ' + e.message, 'error'); }
  }, [showToast]);

  return { configOpen, setConfigOpen, cfgUrl, setCfgUrl, cfgProjectId, setCfgProjectId, cfgUsername, setCfgUsername,
    cfgPassword, setCfgPassword, pwVisible, setPwVisible, savedConfigs, configFormName, setConfigFormName,
    loadConfigs, saveConfig, deleteConfig };
}
