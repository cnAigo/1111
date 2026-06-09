import { useState, useCallback } from 'react';
import request from '../utils/request';

export function useConfig(showToast) {
  const [configOpen, setConfigOpen] = useState(false);
  const [cfgUrl, setCfgUrl] = useState(import.meta.env.VITE_DEFAULT_API_URL || '');
  const [cfgProjectId, setCfgProjectId] = useState(import.meta.env.VITE_DEFAULT_PROJECT_ID || '');
  const [cfgUsername, setCfgUsername] = useState(import.meta.env.VITE_DEFAULT_USERNAME || '');
  const [cfgPassword, setCfgPassword] = useState(import.meta.env.VITE_DEFAULT_PASSWORD || '');
  const [pwVisible, setPwVisible] = useState(false);
  const [savedConfigs, setSavedConfigs] = useState([]);
  const [configFormName, setConfigFormName] = useState('');

  const apiPost = async (url, body) => { const { data } = await request.post(url, body); return data; };
  const apiGet = async (url) => { const { data } = await request.get(url); return data; };
  const apiDelete = async (url) => { const { data } = await request.delete(url); return data; };

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
