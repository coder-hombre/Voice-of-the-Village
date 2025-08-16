# Requirements Document

## Introduction

The Voice of the Village mod enables players to communicate with Minecraft villagers using both voice and text input, with villagers responding in kind. The mod supports two interaction modes (simple and advanced), configurable interaction distances, AI-powered responses, and dynamic villager naming. This feature transforms the traditional villager trading experience into an immersive conversational system.

## Requirements

### Requirement 1

**User Story:** As a Minecraft player, I want to communicate with villagers using my voice, so that I can have more immersive and natural interactions in the game.

#### Acceptance Criteria

1. WHEN a player speaks to a villager using voice input THEN the villager SHALL respond using voice output
2. WHEN a player is in simple mode THEN the player SHALL right-click on a villager to initiate voice communication
3. WHEN a player is in advanced mode THEN the player SHALL hold a configured key while looking at a villager to initiate voice communication
4. WHEN a player is speaking THEN a small icon SHALL appear in the bottom left of the screen to indicate active voice input
5. WHEN a player is within the configured interaction distance THEN voice communication SHALL be enabled
6. WHEN a player is beyond the configured interaction distance THEN voice communication SHALL be disabled

### Requirement 2

**User Story:** As a Minecraft player, I want to communicate with villagers using text input, so that I can interact with them when voice communication is not available or preferred.

#### Acceptance Criteria

1. WHEN a player types to a villager using text input THEN the villager SHALL respond using text output
2. WHEN a player is in simple mode THEN a text input box SHALL be available in the villager trading GUI
3. WHEN a player is in advanced mode THEN the player SHALL use the command "/voice <villagerName> 'message'" to send text messages
4. WHEN a player uses the text command THEN the villagerName parameter SHALL match an existing villager's name within interaction range
5. WHEN a player is within the configured interaction distance THEN text communication SHALL be enabled
6. WHEN a player is beyond the configured interaction distance THEN text communication SHALL be disabled

### Requirement 3

**User Story:** As a Minecraft player, I want to configure the mod's behavior through a config file, so that I can customize the experience to my preferences and setup.

#### Acceptance Criteria

1. WHEN the mod loads THEN it SHALL read configuration values from a config file
2. WHEN a player sets simple mode THEN villager interaction SHALL require right-clicking and use GUI elements
3. WHEN a player sets advanced mode THEN villager interaction SHALL use push-to-talk and commands
4. WHEN a player configures interaction distance THEN it SHALL determine the maximum range for villager communication
5. IF interaction distance is set to negative THEN the feature SHALL be effectively disabled
6. IF interaction distance is set to zero THEN interaction range SHALL be unlimited
7. WHEN a player configures their AI API key THEN it SHALL be used for generating villager responses
8. WHEN a player configures name tag display distance THEN villager names SHALL appear when within that range

### Requirement 4

**User Story:** As a Minecraft player, I want villagers to have unique names displayed above their heads, so that I can identify and reference specific villagers in conversations.

#### Acceptance Criteria

1. WHEN a villager spawns THEN it SHALL be assigned a randomly generated first name from a pool of real names
2. WHEN a player is within the configured name tag distance THEN the villager's name SHALL be displayed above their head
3. WHEN a player is beyond the configured name tag distance THEN the villager's name SHALL not be displayed
4. WHEN a villager is referenced in the "/voice" command THEN the name SHALL match exactly with the villager's assigned name
5. WHEN multiple villagers have similar names THEN each SHALL have a unique identifier or the system SHALL handle name conflicts appropriately

### Requirement 5

**User Story:** As a Minecraft player, I want villagers to provide intelligent responses through AI integration, so that conversations feel natural and contextually appropriate.

#### Acceptance Criteria

1. WHEN a player sends a message to a villager THEN the villager SHALL generate a response using the configured AI service
2. WHEN the AI service is unavailable THEN the system SHALL provide appropriate error handling or fallback responses
3. WHEN a villager responds THEN the response SHALL be contextually appropriate to Minecraft and the villager's role
4. WHEN using voice input THEN the system SHALL convert speech to text for AI processing
5. WHEN generating voice output THEN the system SHALL convert AI text responses to speech
6. WHEN API costs are incurred THEN the system SHALL operate within reasonable usage limits as configured

### Requirement 6

**User Story:** As a Minecraft player, I want villagers to remember our past interactions, so that conversations can build upon previous encounters and create ongoing relationships.

#### Acceptance Criteria

1. WHEN a player interacts with a villager THEN the villager SHALL store the interaction in memory
2. WHEN a villager responds to a player THEN it SHALL reference relevant past interactions when appropriate
3. WHEN the configured memory duration expires THEN old interactions SHALL be automatically removed from villager memory
4. WHEN a player asks about past interactions THEN the villager SHALL be able to recall and discuss previous conversations
5. WHEN the memory retention period is configured THEN it SHALL be measured in Minecraft days with a default of 30 days
6. WHEN the server restarts THEN villager memories SHALL persist and be restored

### Requirement 7

**User Story:** As a Minecraft player, I want villagers to have distinct personalities and gender-appropriate voices, so that each villager feels like a unique individual with their own character.

#### Acceptance Criteria

1. WHEN a villager is assigned a name THEN it SHALL also be assigned a gender based on the name
2. WHEN a villager has a female name THEN it SHALL use a female voice for speech output
3. WHEN a villager has a male name THEN it SHALL use a male voice for speech output
4. WHEN a villager speaks THEN the voice SHALL sound natural and human-like
5. WHEN a villager generates responses THEN they SHALL reflect a consistent personality trait or characteristic
6. WHEN multiple villagers interact THEN each SHALL maintain their distinct personality in conversations

### Requirement 8

**User Story:** As a Minecraft player, I want villagers to react to my behavior through a reputation system, so that my actions have meaningful consequences in the game world.

#### Acceptance Criteria

1. WHEN a player has negative interactions with a villager THEN the villager's opinion of the player SHALL decrease
2. WHEN a villager dislikes a player sufficiently THEN the villager SHALL attack the player once by hitting them
3. WHEN a player's reputation becomes very poor THEN the villager SHALL announce they are going to "hire a guy"
4. WHEN a villager announces hiring help THEN they SHALL spawn an iron golem that is aggressive toward the player
5. WHEN a villager spawns an iron golem THEN they SHALL speak both in-game and through text with a humorous message
6. WHEN a player has positive interactions THEN the villager's opinion SHALL improve accordingly
7. WHEN reputation changes occur THEN they SHALL be persistent and affect future interactions

### Requirement 9

**User Story:** As a Minecraft player, I want to rename villagers using name tags, so that I can give them custom names that are meaningful to me.

#### Acceptance Criteria

1. WHEN a player uses a name tag on a villager THEN the villager's name SHALL be changed to the name tag's text
2. WHEN a villager is renamed THEN the new name SHALL be used in all communication commands and displays
3. WHEN a villager is renamed THEN their gender and voice SHALL be reassigned based on the new name if applicable
4. WHEN a villager is renamed THEN their personality and memory SHALL be preserved
5. WHEN a player uses the "/voice" command THEN it SHALL accept both original and custom names
6. WHEN a renamed villager is referenced THEN the system SHALL use the custom name in all interactions

### Requirement 10

**User Story:** As a Minecraft player, I want the mod to integrate seamlessly with the existing villager trading system, so that communication enhances rather than replaces traditional gameplay mechanics.

#### Acceptance Criteria

1. WHEN a player opens a villager trading GUI THEN communication options SHALL be available alongside trading options
2. WHEN a player is communicating with a villager THEN existing trading functionality SHALL remain accessible
3. WHEN a villager is engaged in conversation THEN it SHALL not interfere with other players' ability to trade
4. WHEN the mod is disabled THEN villagers SHALL behave exactly as in vanilla Minecraft
5. WHEN communication features fail THEN trading functionality SHALL continue to work normally