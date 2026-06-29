import Vue from "vue";
import axios from "axios";

const currentHost = window.location.hostname || "127.0.0.1";
const host = currentHost === "www.wxw.com" || currentHost === "api.wxw.com" ? "localhost" : currentHost;

axios.defaults.baseURL = `http://${host}:10010/api`;
axios.defaults.timeout = 20000;

axios.loadData = async function(url) {
  const resp = await axios.get(url);
  return resp.data;
};

Vue.prototype.$http = axios;
