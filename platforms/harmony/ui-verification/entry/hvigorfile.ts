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
