# Triage/Filtering Implementation Summary

## Overview

Enhanced the bounty triage/filtering system to focus on **simple bugs for quick cash** by:
1. Filtering by language (only languages you know)
2. Filtering by complexity (reject complex issues)
3. Making it configurable

## What Was Added

### 1. Language Pre-Filter

**Fast rejection** before LLM analysis:
- Checks repository language against your `supported-languages` list
- If language not supported → **REJECT immediately** (no LLM call)
- Saves time and LLM costs

**Configuration**:
```yaml
app:
  bounty:
    triage:
      supported-languages: ${TRIAGE_SUPPORTED_LANGUAGES:Java,TypeScript,JavaScript,Python}
```

### 2. Enhanced Complexity Filtering

**Updated LLM prompt** to be more aggressive about rejecting complex issues:

**Rejects**:
- Complex refactoring or architectural changes
- Issues requiring understanding multiple systems
- Vague or poorly described issues
- Issues requiring changes across many files

**Accepts only**:
- Simple bugs (typo, wrong variable, missing check)
- Can be fixed in 1-2 files
- Clear and well-described
- Quick fix (under configured time)

**Configuration**:
```yaml
app:
  bounty:
    triage:
      max-complexity: ${TRIAGE_MAX_COMPLEXITY:simple}  # simple, moderate, or complex
      max-time-minutes: ${TRIAGE_MAX_TIME_MINUTES:60}
      min-confidence: ${TRIAGE_MIN_CONFIDENCE:0.7}
```

### 3. Two-Stage Filtering Process

```
Stage 1: Language Check (Fast)
    ↓
Language supported? → Continue
Language not supported? → REJECT (no LLM call)
    ↓
Stage 2: LLM Complexity Analysis
    ↓
Too complex? → REJECT
Time too high? → REJECT
Confidence too low? → REJECT
All pass? → ACCEPT
```

## Configuration Examples

### Strict (Quick Cash Focus)

```bash
# Only simple bugs in languages you know
TRIAGE_SUPPORTED_LANGUAGES=Java,TypeScript
TRIAGE_MAX_COMPLEXITY=simple
TRIAGE_MAX_TIME_MINUTES=30
TRIAGE_MIN_CONFIDENCE=0.8
```

### Moderate

```bash
# Accept simple and moderate bugs
TRIAGE_SUPPORTED_LANGUAGES=Java,TypeScript,JavaScript,Python
TRIAGE_MAX_COMPLEXITY=moderate
TRIAGE_MAX_TIME_MINUTES=60
TRIAGE_MIN_CONFIDENCE=0.7
```

### Relaxed

```bash
# Accept more complex bugs
TRIAGE_SUPPORTED_LANGUAGES=Java,TypeScript,JavaScript,Python,Go,Rust
TRIAGE_MAX_COMPLEXITY=complex
TRIAGE_MAX_TIME_MINUTES=120
TRIAGE_MIN_CONFIDENCE=0.6
```

## Language Detection

The system determines repository language from:
1. **Database**: If repository is already tracked, uses stored language
2. **Future**: Could infer from repository name or GitHub API

**Language Normalization**:
- Handles aliases: JS → JavaScript, TS → TypeScript, Py → Python
- Case-insensitive matching
- Supports variations

## Logging

All filtering decisions are logged:

```
INFO  - Bounty 123 accepted: confidence=0.85, time=15min, language=Java
DEBUG - Bounty 456 rejected: language 'Rust' not supported. Supported: Java, TypeScript, JavaScript, Python
DEBUG - Bounty 789 rejected: complexity too high: moderate (only simple bugs accepted)
DEBUG - Bounty 101 rejected: estimated time 120 exceeds threshold 60
```

## Benefits

1. **Faster Rejection**: Language check happens before LLM (saves time/cost)
2. **Focus on Simple Bugs**: Aggressive filtering for quick cash opportunities
3. **Configurable**: Adjust thresholds based on your skills and goals
4. **Clear Reasons**: Logs explain why bounties were rejected
5. **Human Verification**: Only processes languages you can verify

## Tuning Tips

1. **Start Strict**: Begin with `max-complexity: simple` and `min-confidence: 0.8`
2. **Monitor Logs**: Check what's being rejected and why
3. **Adjust Gradually**: Loosen thresholds if missing good opportunities
4. **Language List**: Only include languages you can actually verify fixes in
5. **Time Estimates**: Be realistic - if you can't fix it in 30-60 min, reject it

## Files Changed

1. **BountyFilteringService.java**: Added language pre-filter and enhanced complexity analysis
2. **application.yml**: Added triage configuration section
3. **Documentation**: Created triage filtering guide

## Next Steps

1. Configure your supported languages in `.env` or `application.yml`
2. Set complexity threshold (recommend "simple" for quick cash)
3. Monitor logs to see filtering in action
4. Adjust thresholds based on results

