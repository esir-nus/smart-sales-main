// File: platforms/harmony/smartsales-app/entry/hvigorfile.ts
// Module: smartsales-app/entry
// Summary: Entry 模块 hvigor 配置
// Author: restored on 2026-04-21

function resolveHvigorHapTasks(): unknown {
  try {
    return require('@ohos/hvigor-ohos-plugin').hapTasks;
  } catch {
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
