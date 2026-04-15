function resolveHvigorAppTasks(): unknown {
  try {
    return require('@ohos/hvigor-ohos-plugin').appTasks;
  } catch (error) {
    return undefined;
  }
}

const appTasks = resolveHvigorAppTasks();

export default appTasks ? {
  system: appTasks,
  plugins: []
} : {
  plugins: []
};
