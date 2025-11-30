**IMPORTANT**
everytime i invoke this command, the ai should turn whateve i sent into enhanced commit messages in **CHINESE** following the below template rules and then execute git push

🎯 Purpose

This document provides a minimal yet effective commit template for AI-assisted development. Focus on clarity and value over complex processes.

📝 Basic Template

```
<type>: <what you did>

<why you did it>
```

🏷️ Essential Commit Types

· feat - New functionality
· fix - Bug repairs
· docs - Documentation
· style - Code formatting
· refactor - Code improvements
· test - Testing related
· chore - Maintenance tasks

📋 Real-World Examples

Feature Development

```
feat: add user login with email

- Implement email/password authentication
- Add login form validation  
- Store user session securely
```

Bug Fix

```
fix: resolve crash on profile screen

- Handle null user data gracefully
- Add proper error boundaries
- Fix memory leak in image loader
```

Performance Improvement

```
refactor: optimize image loading

- Implement lazy loading for images
- Add memory caching
- Reduce bundle size by 15%
```

Maintenance

```
chore: update dependencies

- Upgrade React to v18.2
- Update security patches
- Remove deprecated packages
```

💡 AI Usage Guidelines

1. Keep titles under 50 characters
2. Use present tense - "add" not "added"
3. Focus on one change per commit
4. Explain the 'why' not just the 'what'
5. Be specific about the value delivered
