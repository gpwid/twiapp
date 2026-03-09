# Contributing to TwiApp 🤝

First off, thanks for taking the time to contribute! TwiApp is a small passion project, and any help making it better, faster, or prettier is hugely appreciated.

Here's a quick guide to help you get started.

## How Can I Contribute?

### 🐛 Reporting Bugs
Found something broken? A video that won't download? An app crash?
1. Check the [Issues](../../issues) tab to see if someone already reported it.
2. If not, open a new issue. 
3. Include what phone you're using, what Android version, the URL that broke the app, and ideally, steps to reproduce the bug.

### 💡 Suggesting Enhancements
Got an idea to make TwiApp better? We're all ears.
1. Open an issue and use the label `enhancement`.
2. Explain what the feature is and *why* it would be useful.

### 💻 Code Contributions
Want to write some code? Awesome!

1. **Fork the repo** and create your branch from `main`.
2. **Write clean code:** Try to stick to the existing Kotlin and Compose conventions in the project.
3. **Test your code:** Make sure your changes don't break existing platforms (try downloading a video from TikTok, YouTube, and IG before submitting).
4. **Open a Pull Request (PR):**
   * Keep your PRs focused on a single feature or bugfix.
   * Provide a clear description of what changed and why.
   * If you changed the UI, a screenshot or screen recording in the PR makes reviewing *much* faster!

## Setting up your environment

1. You'll need Android Studio (the newer, the better).
2. The app uses Jetpack Compose heavily, so some familiarity with declarative UI will help.
3. The core downloading engine is handled by `yt-dlp` (via a Python wrapper for Android). We use [JunkFood02's youtubedl-android fork](https://github.com/JunkFood02/youtubedl-android). If you are looking to fix download issues for specific websites, the fix usually needs to happen in the upstream `yt-dlp` project, not here!

## Code of Conduct

Just be kind and respectful. This is a hobby project meant to be fun and useful. Constructive criticism is welcome; being a jerk is not.

Thanks again for wanting to help out! 🎉
