# GitHub PRs Visualizer

🇬🇧 English | [🇯🇵 日本語](./README.ja.md)

A desktop application for visually understanding the state of open pull requests in GitHub repositories. Inspired by disk space visualizers like WinDirStat and WizTree, it uses a treemap visualization to show at a glance which files and directories are under active development.

## 🖼️ Screenshot

![GitHub PRs Visualizer screenshot](<./docs/images/screenshot-2026-02-20-231900.png>)
*Note: This screenshot shows a development build UI and may differ from future releases.*

## 🎯 Who Should Use This Tool

### For Team Development
- Working with multiple developers simultaneously and want to **identify potential conflicts early**
- Need to prioritize PR reviews
- Want to understand where active development is happening in the codebase

### For AI Agent Users
- Using AI agents to create PRs in parallel and want to **monitor their work ranges**
- Need to visually assess the impact of numerous PRs to prioritize reviews

### For Architecture Improvement
- Frequent conflicts in one area can be a **trigger for architecture review**
- Want to visually verify if your code is properly modularized and files are appropriately divided

## ✨ Key Features

### 📊 Treemap Visualization
- **Files and directories represented by area**: Rectangles sized proportionally to file size (lines of code)
- **Hierarchical display**: Directory structure shown through nesting
- **Zoom functionality**: Use mouse wheel to zoom in/out and explore deeper hierarchies

### 🎨 Change Visualization
- **Color-coded by change type**:
  - 🟢 Green: Additions
  - 🟡 Yellow: Modifications
  - 🔴 Red: Deletions
- **Unique border colors per PR**: Each PR or author gets a distinct color
- **Change density representation**: Color intensity varies based on the proportion of changed lines
- **Conflict warnings**: Highlights when multiple PRs modify the same file

### 📋 PR List Sidebar
- Display all open PRs in a list
- Toggle to show/hide draft PRs
- Individual show/hide toggle for each PR
- Click PR items to open the GitHub PR page in your browser

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher installed
- GitHub OAuth App Client ID
  - Set as `GITHUB_CLIENT_ID` in `.env`

### Running the Application

#### macOS / Linux
```bash
./gradlew :composeApp:run
```

#### Windows
```bash
.\gradlew.bat :composeApp:run
```

### First-Time Setup
1. Launch the application
2. Set your GitHub OAuth App Client ID in the `.env` file:
   ```
   GITHUB_CLIENT_ID=your_github_oauth_client_id_here
   ```
3. Enter the repository owner and repository name
4. Sign in with GitHub from the app
5. Start visualizing PRs

## 💡 Use Cases

### 1. Early Conflict Detection
When multiple developers are working simultaneously, you can identify changes to the same files in advance. By communicating before merging, you can prevent conflicts proactively.

### 2. AI Agent Work Monitoring
When multiple AI agents are creating PRs in parallel, you can visually monitor each agent's work range. This helps you detect overlaps or unintended interference early.

### 3. Architecture Health Check
Files or directories with frequent conflicts might signal a need for design improvements. Through visualization, you can identify opportunities for better modularization and file organization.

### 4. Review Prioritization
By prioritizing PRs with large impact areas or significant overlaps with other PRs, you can establish a more efficient code review process.

## 🔧 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Compose Multiplatform (Compose for Desktop)
- **Rendering Engine**: Skia
- **Networking**: Ktor (GitHub API communication)
- **Architecture**: MVVM

## 📦 Future Distribution

In the future, we plan to distribute pre-built applications through GitHub release tags, making it easy to use without building locally.

## 🤝 Contributing

Contributions are welcome! Feel free to report bugs, request features, or submit pull requests.

## 🔗 Documentation

For detailed specifications and design documents, see the [docs directory](./docs).
