import re

with open('app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt', 'r') as f:
    content = f.read()

# Make class implement IAgentViewModel
content = content.replace(') : ViewModel() {', ') : ViewModel(), IAgentViewModel {')

# Add overrides to vals
vals_to_override = [
    'val agentActivity =',
    'val uiState =',
    'val inputText =',
    'val isSending =',
    'val errorMessage =',
    'val toastMessage =',
    'val history =',
    'val taskBoardItems =',
    'val sessionTitle =',
    'val heroUpcoming =',
    'val heroAccomplished =',
    'val mascotState =',
    'val currentDisplayName:',
    'val heroGreeting ='
]
for v in vals_to_override:
    content = re.sub(r'(?<!override )' + re.escape(v), 'override ' + v, content)

# Add overrides to funs
funs_to_override = [
    'fun clearToast(',
    'fun updateInput(',
    'fun clearError(',
    'fun amendAnalystPlan(',
    'fun interactWithMascot(',
    'fun updateSessionTitle(',
    'fun selectTaskBoardItem(',
    'fun confirmAnalystPlan(',
    'fun send(',
    'fun debugRunScenario('
]
for f_sig in funs_to_override:
    content = re.sub(r'(?<!override )' + re.escape(f_sig), 'override ' + f_sig, content)

with open('app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt', 'w') as f:
    f.write(content)
print("AgentViewModel refactored successfully.")
