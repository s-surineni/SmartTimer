# SmartTimer - Android Timer Management App

A modern Android application built with Jetpack Compose that allows users to create and manage multiple timers organized in groups with swipe navigation.

## Features

### 🕒 Timer Management
- **Multiple Timers**: Create unlimited timers with custom names
- **Predefined Durations**: Quick selection from common time intervals (5min, 10min, 15min, 30min, 1hr, 2hr)
- **Custom Duration**: Set custom timer durations in minutes
- **Real-time Countdown**: Live countdown with progress indicators
- **Background Operation**: Timers continue running even when app is in background

### 📁 Group Organization
- **Timer Groups**: Organize timers into color-coded groups
- **Group Management**: Add, delete, and manage timer groups
- **Color Coding**: Each group has a unique color for easy identification
- **Group Statistics**: See timer count per group

### 🔄 Swipe Navigation
- **Horizontal Swiping**: Swipe left/right to navigate between timer groups
- **Group Indicators**: Visual chips showing all groups with current selection
- **Smooth Animations**: Fluid transitions between groups

### 🎨 Modern UI
- **Material Design 3**: Latest Material Design components and theming
- **Dark/Light Theme**: Automatic theme switching based on system preference
- **Responsive Design**: Optimized for different screen sizes
- **Accessibility**: Screen reader support and keyboard navigation

## Technical Architecture

### Built With
- **Kotlin**: Modern Android development language
- **Jetpack Compose**: Declarative UI toolkit
- **Room Database**: Local data persistence with SQLite
- **ViewModel & LiveData**: Reactive UI state management
- **Coroutines**: Asynchronous programming
- **Material 3**: Latest Material Design components

### Architecture Components
- **MVVM Pattern**: Model-View-ViewModel architecture
- **Repository Pattern**: Clean data access layer
- **Service Layer**: Background timer management
- **Dependency Injection**: Manual DI for simplicity

## Project Structure

```
app/src/main/java/com/example/smarttimer/
├── data/                    # Data layer
│   ├── Timer.kt            # Timer entity
│   ├── TimerGroup.kt       # Timer group entity
│   ├── TimerDao.kt         # Database access object
│   ├── TimerDatabase.kt    # Room database
│   └── TimerRepository.kt # Repository pattern
├── service/                 # Background services
│   └── TimerService.kt     # Timer management service
├── ui/                     # UI layer
│   ├── MainViewModel.kt    # Main screen view model
│   ├── TimerViewModel.kt   # Timer management view model
│   ├── components/         # Reusable UI components
│   ├── screens/            # Screen composables
│   └── theme/              # App theming
└── MainActivity.kt         # Main activity
```

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (API level 24)
- Kotlin 1.9.0+

### Installation
1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build and run the app

### Usage

#### Creating Timer Groups
1. Tap the "+" button in the top app bar
2. Enter a group name
3. Select a color from the color palette
4. Tap "Add Group"

#### Adding Timers
1. Navigate to a timer group
2. Tap the "+" button in the group header
3. Enter timer name
4. Choose predefined duration or enter custom minutes
5. Tap "Add Timer"

#### Managing Timers
- **Start Timer**: Tap the "Start" button on any timer
- **Stop Timer**: Tap the "Stop" button on running timers
- **Delete Timer**: Tap the delete icon on timer cards
- **Delete Group**: Tap the delete icon in group headers

#### Navigation
- **Swipe Left/Right**: Navigate between timer groups
- **Tap Group Chips**: Quick navigation to specific groups

## Key Features Implementation

### Background Timer Service
The app uses a foreground service to maintain timer state even when the app is not in focus. This ensures:
- Timers continue running in background
- Notification shows active timer count
- Timer state persists across app restarts

### Room Database
Local SQLite database using Room for:
- Timer and group persistence
- Foreign key relationships
- Automatic data synchronization

### Swipe Navigation
Horizontal pager implementation with:
- Smooth swipe gestures
- Visual group indicators
- State management for current group

### Material Design 3
Modern UI components including:
- Dynamic color theming
- Adaptive layouts
- Accessibility features

## Future Enhancements

- [ ] Timer sound notifications
- [ ] Timer repeat functionality
- [ ] Timer templates
- [ ] Cloud synchronization
- [ ] Widget support
- [ ] Timer sharing
- [ ] Statistics and analytics
- [ ] Multiple timer simultaneous running
- [ ] Timer categories and tags

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Material Design 3 guidelines
- Jetpack Compose documentation
- Android Room persistence library
- Kotlin coroutines for async programming
