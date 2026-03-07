# FurnitureFit - Design Studio

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.java.net/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

FurnitureFit is a comprehensive desktop application designed for furniture designers and interior decorators to create, visualize, and manage room layouts. Built with modern Java technologies, it provides an intuitive interface for designing spaces in both 2D and 3D perspectives, complete with furniture placement, lighting controls, and collaborative features.

## Table of Contents

- [Features](#features)
- [Technologies Used](#technologies-used)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [Building from Source](#building-from-source)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

## Features

### Core Functionality
- **User Authentication**: Secure login and registration system with SQLite database persistence
- **Design Dashboard**: Manage multiple design projects with easy access to saved layouts
- **2D & 3D Visualization**: Switch between 2D top-down and 3D perspective views for comprehensive design visualization
- **Furniture Library**: Extensive collection of furniture items with drag-and-drop placement
- **Room Configuration**: Customize room dimensions, walls, and flooring options
- **Lighting Controls**: Adjust lighting settings to see how designs look under different conditions
- **Properties Panel**: Fine-tune object properties including position, rotation, and scale
- **Undo/Redo System**: Full history management for design modifications
- **Zoom Controls**: Precise zooming and panning for detailed work
- **Save/Load Designs**: Persistent storage of designs with JSON serialization

### User Experience
- **Modern UI**: Clean, professional interface using FlatLaf theme with rounded components
- **Responsive Design**: Optimized for various screen sizes with minimum 1024x700 resolution
- **Intuitive Navigation**: Card-based layout switching between different application views
- **Help System**: Integrated help overlay and documentation
- **Account Management**: User profile management and settings

### Technical Features
- **Cross-Platform**: Runs on Windows, macOS, and Linux
- **Hardware Acceleration**: OpenGL-based 3D rendering for smooth performance
- **Database Integration**: SQLite for reliable local data storage
- **Modular Architecture**: Clean separation of concerns with MVC pattern

## Technologies Used

- **Java 21**: Core programming language with modern features
- **Swing**: GUI framework for desktop application
- **JOGL (Java OpenGL)**: 3D graphics rendering
- **FlatLaf**: Modern look and feel for Swing applications
- **MigLayout**: Flexible layout manager for complex UIs
- **SQLite**: Embedded database for data persistence
- **Gson**: JSON serialization for design files
- **Maven**: Build automation and dependency management

## Prerequisites

Before running FurnitureFit, ensure you have the following installed:

- **Java Development Kit (JDK) 21** or later
  - Download from [OpenJDK](https://openjdk.java.net/) or [Adoptium](https://adoptium.net/)
- **Apache Maven 3.8+** for building the project
  - Download from [Maven Apache](https://maven.apache.org/download.cgi)
- **Git** for cloning the repository (optional)

### System Requirements
- **Operating System**: Windows 10+, macOS 10.14+, or Linux
- **RAM**: Minimum 4GB, recommended 8GB+
- **Graphics**: OpenGL 3.3+ compatible graphics card for 3D rendering
- **Storage**: 100MB free space for application and data

## Installation

### Option 1: Download Pre-built JAR (Recommended for End Users)

1. Download the latest `FurnitureFit.jar` from the [Releases](https://github.com/Kavidu23/FurnitureFit/releases) page
2. Ensure Java 21+ is installed on your system
3. Double-click the JAR file to run, or execute from command line:
   ```bash
   java -jar FurnitureFit.jar
   ```

### Option 2: Build from Source (For Developers)

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Kavidu23/FurnitureFit.git
   cd FurnitureFit
   ```

2. **Build the project**:
   ```bash
   mvn clean compile
   ```

3. **Run the application**:
   ```bash
   mvn exec:java
   ```

## Usage

### Getting Started

1. **Launch the Application**: Run the JAR file or use Maven as described above
2. **Create Account**: Register a new account or login with existing credentials
3. **Access Dashboard**: View and manage your design projects

### Creating a Design

1. **New Design**: Click "New Design" from the dashboard
2. **Configure Room**: Use the room configuration dialog to set dimensions and properties
3. **Add Furniture**: Open the furniture picker and drag items into the design area
4. **Adjust Properties**: Use the properties panel to fine-tune object placement and appearance
5. **Switch Views**: Toggle between 2D and 3D views using the toolbar
6. **Lighting**: Adjust lighting settings for realistic visualization
7. **Save**: Save your design for future editing

### Keyboard Shortcuts

- `Ctrl+Z`: Undo
- `Ctrl+Y`: Redo
- `R`: Rotate

### Tips for Best Experience

- Use the 3D view for final presentations and lighting adjustments
- Save frequently to avoid losing work
- Experiment with different lighting conditions to see how designs appear in various environments
- Use the zoom controls for precise placement of small objects

## Building from Source

### Full Build Process

```bash
# Clone repository
git clone https://github.com/Kavidu23/FurnitureFit.git
cd FurnitureFit

# Clean and compile
mvn clean compile

# Run tests (if available)
mvn test

# Package into runnable JAR
mvn package

# The executable JAR will be in target/FurnitureFit.jar
```

### Development Setup

1. Import the project into your preferred IDE (IntelliJ IDEA, Eclipse, VS Code)
2. Ensure JDK 21 is configured as the project SDK
3. Run `mvn compile` to download dependencies
4. Use your IDE's run configuration to launch `com.mycompany.furniturefit.FurnitureFit`

## Project Structure

```
FurnitureFit/
├── pom.xml                          # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/com/mycompany/furniturefit/
│   │   │   ├── FurnitureFit.java     # Main application entry point
│   │   │   ├── db/                   # Database layer
│   │   │   │   ├── DatabaseManager.java
│   │   │   │   ├── DesignDAO.java
│   │   │   │   └── UserDAO.java
│   │   │   ├── graphics/             # Graphics and rendering
│   │   │   │   ├── Canvas2DPanel.java
│   │   │   │   ├── Canvas3DPanel.java
│   │   │   │   ├── OpenGLCanvas3D.java
│   │   │   │   └── RenderUtils.java
│   │   │   ├── model/                # Data models
│   │   │   │   ├── Design.java
│   │   │   │   ├── Furniture.java
│   │   │   │   ├── Room.java
│   │   │   │   └── User.java
│   │   │   └── ui/                   # User interface components
│   │   │       ├── MainFrame.java
│   │   │       ├── LoginPanel.java
│   │   │       ├── DashboardPanel.java
│   │   │       ├── DesignEditorPanel.java
│   │   │       └── ... (other UI panels)
│   │   └── resources/                # Static resources
│   │       ├── icons/
│   │       └── images/
│   └── test/                         # Unit tests
└── target/                           # Build output
```

## Contributing

We welcome contributions to FurnitureFit! Please follow these guidelines:

### Development Process

1. **Fork the repository** on GitHub
2. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes** following the existing code style
4. **Test thoroughly** - ensure the application builds and runs correctly
5. **Commit your changes**:
   ```bash
   git commit -m "Add: Brief description of your changes"
   ```
6. **Push to your fork** and **create a pull request**

### Code Style Guidelines

- Follow Java naming conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public methods
- Keep methods focused on single responsibilities
- Handle exceptions appropriately

### Reporting Issues

- Use GitHub Issues to report bugs or request features
- Provide detailed steps to reproduce bugs
- Include system information (OS, Java version, etc.)
- Attach screenshots for UI-related issues

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

**Project Maintainer**: Rateralalage Thialakarathna

- **GitHub**: [https://github.com/Kavidu23/FurnitureFit](https://github.com/Kavidu23/FurnitureFit)
- **Issues**: [https://github.com/Kavidu23/FurnitureFit/issues](https://github.com/Kavidu23/FurnitureFit/issues)

For questions or support, please open an issue on GitHub.

---

*Built with ❤️ using Java and modern desktop technologies*