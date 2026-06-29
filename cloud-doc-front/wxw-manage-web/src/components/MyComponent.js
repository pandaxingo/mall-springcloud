import m from './messages'

const MyComponent = {};

MyComponent.install = function (Vue) {
  Vue.component("vTree", () => import('./tree/Tree'));
  Vue.component("vCascader", () => import('./cascader/Cascader'));
  Vue.component("vUpload", () => import('./form/Upload'));
  Vue.component("vEditor", () => import('./form/Editor'));
  Vue.prototype.$message = m;
  Vue.prototype.$format = function (val) {
    if(typeof val === 'string'){
      if(isNaN(val)){
        return null;
      }
      return Number(val);
    }else if(typeof val === 'number'){
      if(val == null){
        return null;
      }
      return Number(val).toFixed(2).replace(/\.00$/, "");
    }
  }
}


Object.deepCopy = function (src) {
  // for(let key in src){
  //   if(!src[key]){
  //     continue;
  //   }
  //   if(src[key].constructor === Array){
  //     dest[key] = [];
  //     Object.deepCopy(src[key],dest[key])
  //   }else if(typeof src[key] === 'object'){
  //     dest[key] = {};
  //     Object.deepCopy(src[key],dest[key])
  //   }
  //   dest[key] = src[key];
  // }
  return JSON.parse(JSON.stringify(src));
}

export default MyComponent;
