// 模块级 hvigorfile — entry 模块
// 使用 require 方式兼容 CommonJS 执行环境 (hvigorw 不支持 import.meta)

function resolveHvigorHapTasks() {
  try {
    return require('@ohos/hvigor-ohos-plugin').hapTasks;
  } catch (error) {
    return undefined;
  }
}

const hapTasks = resolveHvigorHapTasks();

export default hapTasks ? {
  system: hapTasks,
  plugins: []
} : {
  plugins: []
};
