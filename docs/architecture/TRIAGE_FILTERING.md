# Bounty Triage and Filtering

## Overview

The triage/filtering system eliminates issues that are too complex or in languages you can't verify, focusing on **simple bugs for quick cash**.

## Two-Stage Filtering

### Stage 1: Language Pre-Filter (Fast, No LLM)

**Purpose**: Immediately reject bounties in languages you don't know.

**How it works**:
1. Get repository language from database
2. Check if language is in your `supported-languages` list
3. If not supported → **REJECT immediately** (no LLM call needed)

**Benefits**:
- Fast rejection (no LLM cost)
- Clear reason: "Language 'Rust' not supported"
- Saves time and resources

### Stage 2: Complexity & Feasibility Analysis (LLM)

**Purpose**: Use LLM to evaluate if the bug is simple enough for quick fixes.

**Evaluation Criteria** (ALL must be true):
1. **SIMPLICITY**: Simple bug fix (typo, wrong variable, missing null check) vs complex refactoring
2. **QUICK FIX**: Can be fixed in under configured time (default: 60 minutes)
3. **CLEAR ISSUE**: Problem is clearly described and easy to understand
4. **LOW COMPLEXITY**: Requires understanding only 1-2 files, not entire codebase
5. **NO ARCHITECTURE**: Doesn't require understanding system architecture or design patterns

**Rejects if**:
- Requires understanding multiple interconnected systems
- Needs architectural changes or refactoring
- Involves complex algorithms or data structures
- Requires deep domain knowledge
- Needs extensive testing or documentation
- Is vague or poorly described
- Requires changes across many files

**Accepts only if**:
- Simple bug (typo, wrong value, missing check, etc.)
- Can be fixed in 1-2 files
- Clear and well-described issue
- Quick fix (under configured time)
- High confidence we can solve it

## Configuration

### application.yml

```yaml
app:
  bounty:
    triage:
      # Languages you know and can human-verify
      supported-languages: ${TRIAGE_SUPPORTED_LANGUAGES:Java,TypeScript,JavaScript,Python}
      
      # Maximum complexity: simple, moderate, or complex
      # For quick cash, use "simple" to reject moderate/complex issues
      max-complexity: ${TRIAGE_MAX_COMPLEXITY:simple}
      
      # Maximum time estimate (minutes)
      max-time-minutes: ${TRIAGE_MAX_TIME_MINUTES:60}
      
      # Minimum confidence (0.0-1.0)
      min-confidence: ${TRIAGE_MIN_CONFIDENCE:0.7}
```

### Environment Variables

```bash
# Set your known languages (comma-separated)
export TRIAGE_SUPPORTED_LANGUAGES="Java,TypeScript,JavaScript,Python"

# Set complexity threshold (simple, moderate, complex)
export TRIAGE_MAX_COMPLEXITY="simple"

# Set time threshold (minutes)
export TRIAGE_MAX_TIME_MINUTES=60

# Set confidence threshold (0.0-1.0)
export TRIAGE_MIN_CONFIDENCE=0.7
```

## Language Detection

The system determines repository language from:

1. **Database**: If repository is already tracked, uses stored language
2. **Future**: Could infer from repository name, file extensions, or GitHub API

**Language Normalization**:
- Handles common aliases (JS → JavaScript, TS → TypeScript, Py → Python)
- Case-insensitive matching
- Supports variations (TypeScript, typescript, TS, ts)

## Complexity Levels

### Simple ✅ (Recommended for Quick Cash)
- Typo fixes
- Wrong variable names
- Missing null checks
- Simple logic errors
- Single file changes
- Clear, well-described issues

**Example**: "Fix typo in variable name: `userNmae` should be `userName`"

### Moderate ⚠️ (Rejected if max-complexity=simple)
- Requires understanding 2-3 files
- Needs some refactoring
- Involves multiple functions/methods
- Requires understanding basic patterns

**Example**: "Fix authentication bug that requires changes to AuthService and UserService"

### Complex ❌ (Always Rejected)
- Architectural changes
- Requires deep system understanding
- Multiple interconnected systems
- Complex algorithms
- Extensive refactoring

**Example**: "Refactor authentication system to support OAuth2 and SAML"

## Filtering Flow

```
Bounty Discovered (Algora/Polar.sh)
    ↓
Language Pre-Filter
    ├─→ Language not supported? → REJECT (fast, no LLM)
    └─→ Language supported? → Continue
    ↓
LLM Complexity Analysis
    ├─→ Too complex? → REJECT
    ├─→ Time estimate too high? → REJECT
    ├─→ Confidence too low? → REJECT
    └─→ All checks pass? → ACCEPT
    ↓
Enqueue for Processing
```

## Examples

### ✅ Accepted: Simple Bug

**Issue**: "Fix typo: `userNmae` should be `userName` in UserService.java"

**Language**: Java ✅ (supported)
**Complexity**: Simple ✅
**Time Estimate**: 5 minutes ✅
**Confidence**: 0.95 ✅

**Result**: ACCEPTED

### ❌ Rejected: Wrong Language

**Issue**: "Fix memory leak in Rust codebase"

**Language**: Rust ❌ (not in supported-languages)
**Result**: REJECTED immediately (no LLM call)

### ❌ Rejected: Too Complex

**Issue**: "Refactor authentication system to support multiple providers"

**Language**: Java ✅ (supported)
**Complexity**: Complex ❌ (exceeds "simple" threshold)
**Result**: REJECTED

### ❌ Rejected: Time Estimate Too High

**Issue**: "Fix bug in distributed system coordination"

**Language**: TypeScript ✅ (supported)
**Complexity**: Moderate
**Time Estimate**: 120 minutes ❌ (exceeds 60 minute threshold)
**Result**: REJECTED

## Tuning for Quick Cash

To maximize quick cash opportunities:

1. **Set `max-complexity: simple`** - Only accept simple bugs
2. **Set `max-time-minutes: 30-60`** - Reject anything taking longer
3. **Set `min-confidence: 0.7-0.8`** - Only high-confidence fixes
4. **Limit `supported-languages`** - Only languages you can verify
5. **Monitor rejection reasons** - Adjust thresholds based on what you're missing

## Logging

The system logs all filtering decisions:

```
INFO  - Bounty 123 accepted: confidence=0.85, time=15min, language=Java
DEBUG - Bounty 456 rejected: language 'Rust' not supported. Supported: Java, TypeScript, JavaScript, Python
DEBUG - Bounty 789 rejected: complexity too high: moderate (only simple bugs accepted)
DEBUG - Bounty 101 rejected: estimated time 120 exceeds threshold 60
```

## Best Practices

1. **Start Strict**: Begin with `max-complexity: simple` and `min-confidence: 0.8`
2. **Monitor Results**: Check logs to see what's being rejected
3. **Adjust Gradually**: Loosen thresholds if you're missing good opportunities
4. **Language List**: Only include languages you can actually verify fixes in
5. **Time Estimates**: Be realistic - if you can't fix it in 30-60 min, reject it

## Future Enhancements

1. **Learning from Rejections**: Track why bounties were rejected and learn patterns
2. **Language Inference**: Auto-detect language from repository if not in database
3. **Complexity Scoring**: More granular complexity scoring (1-10 scale)
4. **Historical Success Rate**: Track success rate by complexity/language to tune thresholds

