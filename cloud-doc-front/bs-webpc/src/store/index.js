/*
 * @Descripttion: Vuex入口
 * @version: V 1.0
 * @Author: twx
 * @Date: 2020-04-13 16:29:18
 */
import Vue from "vue";
import Vuex from "vuex";
import user from "./modules/user";

Vue.use(Vuex);

const store = new Vuex.Store({
  strict: true,
  modules: {
    user
  }
});
export default store;
