// State and DOM Elements
const mascotIsland = document.getElementById('mascot-island');
const mascotText = document.getElementById('mascot-text');
const mascotIcon = document.querySelector('.mascot-icon');
const chatFeed = document.getElementById('chat-feed');
const inputField = document.getElementById('unified-input');
const taskBoard = document.getElementById('task-board');
const taskBoardContent = document.getElementById('task-board-content');
const sendBtn = document.getElementById('send-btn');

let mascotTimeout = null;

// --- System I: The Mascot (Ephemeral) ---
function showMascot(text, icon = '✨', duration = 4000) {
  if (mascotTimeout) {
    clearTimeout(mascotTimeout);
  }
  
  mascotText.innerHTML = text;
  mascotIcon.innerHTML = icon;
  mascotIsland.classList.remove('hidden');
  
  mascotTimeout = setTimeout(() => {
    mascotIsland.classList.add('hidden');
  }, duration);
}

// --- System II: Prism Orchestrator (Persistent) ---
function addChatBubble(text, sender = 'user', isThinking = false) {
  const bubble = document.createElement('div');
  bubble.className = `bubble ${sender}-bubble`;
  if (isThinking) {
    bubble.classList.add('thinking-bubble');
    bubble.innerHTML = `<span class="dot"></span><span class="dot"></span><span class="dot"></span>`;
  } else {
    bubble.innerHTML = text;
  }
  chatFeed.appendChild(bubble);
  scrollToBottom();
  return bubble;
}

function scrollToBottom() {
  setTimeout(() => {
    chatFeed.scrollTop = chatFeed.scrollHeight;
  }, 50);
}

// Helper: Show Task Board Launchpad
function showLaunchpad() {
  const tasks = [
    { icon: '📝', label: 'Summarize Intent' },
    { icon: '🗣️', label: 'Talk Simulator' },
    { icon: '📄', label: 'Export PDF' }
  ];
  taskBoardContent.innerHTML = '';
  tasks.forEach(t => {
    const chip = document.createElement('div');
    chip.className = 'task-chip';
    chip.innerHTML = `<span class="chip-icon">${t.icon}</span> ${t.label}`;
    
    chip.addEventListener('click', () => {
      taskBoard.classList.add('hidden');
      addChatBubble(`Launch: ${t.label}`, 'user');
      const thinking = addChatBubble('', 'assistant', true);
      setTimeout(() => {
        thinking.classList.remove('thinking-bubble');
        thinking.innerHTML = `Executing <strong>${t.label}</strong> workflow...`;
      }, 1500);
    });
    
    taskBoardContent.appendChild(chip);
  });
  taskBoard.classList.remove('hidden');
}

// --- Lightning Router ---
function lightningRouter(input) {
  const text = input.trim().toLowerCase();
  
  // Hide task board on new input
  taskBoard.classList.add('hidden');

  // Intent Classification
  const isGreeting = /^(hello|hi|hey|good morning|thanks|thank you|greetings)/.test(text);
  const isNoise = /^[^a-z0-9\s]+$/.test(text) || text === "asdf" || (text.length < 3 && !/^[a-z0-9]+$/i.test(text));
  const isDeepTask = /(analyze|summarize|report|deep scan|scan)/.test(text);
  
  if (isGreeting) {
    // System I
    showMascot("Hello there! Ready to tackle today's CRM tasks?", "👋");
  } else if (isNoise) {
    // System I
    showMascot("Oops, looks like a typo! Need help looking up a client?", "🤔");
  } else if (isDeepTask) {
    // System II - Deep Analysis
    addChatBubble(input, 'user');
    const thinking = addChatBubble('', 'assistant', true);
    
    setTimeout(() => {
      thinking.classList.remove('thinking-bubble');
      thinking.innerHTML = `<strong>Baseline Summary Complete</strong><br>I have extracted the core data points from the requested context. Please select a specialized workflow below to proceed with the analysis.`;
      showLaunchpad();
      scrollToBottom();
    }, 1800);
    
  } else {
    // System II - Simple QA
    addChatBubble(input, 'user');
    const thinking = addChatBubble('', 'assistant', true);
    
    setTimeout(() => {
      thinking.classList.remove('thinking-bubble');
      thinking.innerHTML = `I can certainly help with that. Could you provide a bit more context or a specific entity name?`;
      scrollToBottom();
    }, 1200);
  }
}

// --- Event Listeners ---
function handleSubmit() {
  const text = inputField.value;
  if (!text) return;
  
  lightningRouter(text);
  inputField.value = "";
}

sendBtn.addEventListener('click', handleSubmit);
inputField.addEventListener('keypress', (e) => {
  if (e.key === 'Enter') {
    handleSubmit();
  }
});

// Update Demo Buttons to act as shortcuts filling the input
document.getElementById('btn-demo-1').addEventListener('click', () => {
    inputField.value = "Good morning";
    handleSubmit();
});
document.getElementById('btn-demo-2').addEventListener('click', () => {
    inputField.value = "*U*#)$878423";
    handleSubmit();
});
document.getElementById('btn-demo-3').addEventListener('click', () => {
    inputField.value = "What is my next meeting?";
    handleSubmit();
});
document.getElementById('btn-demo-4').addEventListener('click', () => {
    inputField.value = "Analyze the last project report";
    handleSubmit();
});

// Reset Chat
document.getElementById('btn-reset').addEventListener('click', () => {
  chatFeed.innerHTML = `
    <div class="chat-greeting formal-mode">
      <h3>Prism Orchestrator</h3>
      <p>How can I help you today?</p>
    </div>
  `;
  mascotIsland.classList.add('hidden');
  taskBoard.classList.add('hidden');
  inputField.value = '';
});

// Demo a proactive mascot greeting on load
setTimeout(() => {
  showMascot("Welcome back! You have 3 tasks pending today.", "✨", 5000);
}, 800);
