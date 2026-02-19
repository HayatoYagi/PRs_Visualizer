# Debugging Guide for .github Directory Recognition Issue

## Issue Description
Issue #15: `.github` directory is not being recognized as a directory in the visualizer.

## Investigation Summary

### Code Analysis
I've thoroughly analyzed the codebase and found that the logic for building the directory tree appears to be correct:

1. **File Fetching** (`fetchRepositoryFiles`): Correctly fetches all files from GitHub API's recursive tree endpoint
2. **Directory Inference** (`buildTree`): Correctly infers directory structure from file paths
3. **Path Extraction**: Uses `substringBeforeLast('/')` and `substringAfterLast('/')` which should work correctly for paths like `.github/workflows/build.yml`

### What Should Happen
For a file like `.github/workflows/build.yml`:
1. GitHub API returns the file (type: "blob")
2. Parent path is extracted: `.github/workflows`
3. `ensureDir(".github/workflows")` is called
4. This recursively creates:
   - `.github/workflows` (parent: `.github`, name: `workflows`)
   - `.github` (parent: `""` (root), name: `.github`)

### Diagnostic Logging Added
To help identify the actual issue, I've added comprehensive logging in `GitHubApi.kt`:

#### 1. File Processing Log
```
GitHubApi: Processing hidden file: <path>
GitHubApi: Total files processed: X, hidden files: Y
```

#### 2. Directory Creation Log
```
GitHubApi: Creating hidden directory: path='<path>', name='<name>', parent='<parent>'
GitHubApi: Total directories created: X, hidden directories: Y
GitHubApi: Directories: [list of hidden directory paths]
```

#### 3. Tree Validation Log
```
GitHubApi: Frozen tree contains X hidden directories
```

#### 4. Error Detection
```
GitHubApi: ERROR - Empty directory name for path: '<path>'
```

## How to Diagnose

### Step 1: Run the Application
```bash
./gradlew :composeApp:run
```

### Step 2: Load a Repository with .github Directory
1. Open the application
2. Authenticate with GitHub
3. Load this repository (HayatoYagi/GitHub_PRs_Visualizer) which contains `.github/workflows/build.yml`

### Step 3: Check Console Output
Look for the diagnostic logs:

**Expected Output:**
```
GitHubApi: Processing hidden file: .github/workflows/build.yml
GitHubApi: Processing hidden file: .gitignore
GitHubApi: Processing hidden file: .env
GitHubApi: Total files processed: X, hidden files: 3
GitHubApi: Creating hidden directory: path='.github', name='.github', parent=''
GitHubApi: Total directories created: Y, hidden directories: 2
GitHubApi: Directories: [.github, .github/workflows]
GitHubApi: Frozen tree contains 2 hidden directories
```

**If you see different output**, it will help identify where the issue occurs:
- If hidden files are NOT being logged → Issue is in `fetchRepositoryFiles`
- If hidden directories are NOT being created → Issue is in `ensureDir`
- If hidden directories are created but count is 0 in frozen tree → Issue is in `freeze` function
- If ERROR message appears → There's a bug in path extraction

### Step 4: Check UI
After confirming directories are created correctly in logs:
1. Check if `.github` appears in the file explorer pane
2. Check if `.github` appears in the treemap visualization
3. Try clicking on `.github` to navigate into it

## Potential Root Causes

Based on the code analysis, here are the most likely issues:

### 1. GitHub API Issue (Low Probability)
- GitHub API might not return files in `.github` directory
- **Test**: Check if files are logged in Step 3

### 2. Path Extraction Bug (Low Probability)
- There might be an edge case in string manipulation
- **Test**: Check if directories are created with correct names in logs

### 3. UI Display Issue (Medium Probability)
- Directories might be created correctly but not displayed
- **Test**: Check if logs show directories being created, but UI doesn't show them

### 4. Sorting/Filtering Issue (Medium Probability)
- Hidden directories might be filtered out during display
- **Test**: Check explorer pane and treemap for `.github`

### 5. Unknown Issue (High Probability)
- There's a subtle bug that only manifests at runtime
- **Solution**: The logs should help identify it

## Next Steps

1. **Run the application** with the diagnostic logging
2. **Collect and analyze** the console output
3. **Compare** actual behavior vs expected behavior
4. **Identify** the exact point of failure
5. **Implement** targeted fix based on findings
6. **Remove** or reduce diagnostic logging once issue is fixed

## Testing Checklist

After implementing a fix:
- [ ] .github directory appears in file explorer
- [ ] .github directory can be navigated into
- [ ] .github directory appears in treemap
- [ ] .github/workflows subdirectory is accessible  
- [ ] Files in .github/workflows are visible
- [ ] Test with multiple repositories containing .github
- [ ] Test with other hidden directories (e.g., .vscode, .idea)
- [ ] Verify no performance degradation from logging

## Files Modified

1. `composeApp/src/jvmMain/kotlin/io/github/hayatoyagi/prvisualizer/github/GitHubApi.kt`
   - Added diagnostic logging
   - Added validation checks
   
2. `composeApp/src/jvmTest/kotlin/io/github/hayatoyagi/prvisualizer/GitHubApiTest.kt`
   - Added unit tests for path extraction logic

## Contact
For questions or if you need help interpreting the logs, please comment on Issue #15 or PR #16.
