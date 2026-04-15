# Plugin Architecture Deep Dive: The Gateway model vs Streamlined Pipelines

## The Reality of Multi-Turn Plugin Scenarios
The user highlighted core realities of conversational AI.
- Plugins are often continuations of an existing session
- A plugin may generate a PDF but require the CRM database and Entity Writer.
- Another plugin might switch the mode entirely (e.g. Negotiation Simulator) and require a continuous session stream.

## Why 'Naked Plugins' Failed
- They create boilerplate hell: querying DAOs, getting permissions, transaction management.
- They have a massive blast radius.

## The Solution: A Unified `PluginGateway`
This is how we give developers the "socket" metaphor without exposing the raw infrastructure.
### The Highway and Town Analogy
The user's analogy of the "Main Road" (Unified Pipeline) branching into different "Towns" (Plugins) is perfect.
- The car (the session context) is the same.
- The passenger (the user) is the same.
- The underlying engine (the Core Modules) is the same.

The real engineering challenge here isn't the plugins themselves (like a simple PDF generator). The challenge is **defining the robust communication protocol**—the Access Key or the "Socket"—that lets the plugins tap into the engine's power without breaking it.

### The "Access Key" Protocol
To achieve this black-box interaction, we define an access protocol that every plugin must declare when it registers, and a strictly controlled Gateway interface that provides only what was requested.
