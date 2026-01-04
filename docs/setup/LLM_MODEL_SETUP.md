# LLM Model Setup

## Recommended Model: DeepSeek Coder 6.7B

Based on research (January 2026), **DeepSeek Coder 6.7B** is the recommended model for code review and bug fixing tasks when using Ollama for local inference.

### Why DeepSeek Coder 6.7B?

- **Code-Optimized**: Trained specifically on code datasets
- **Good Balance**: Excellent balance of accuracy and speed
- **Cost-Effective**: Free when self-hosted via Ollama
- **Low Hallucination**: Better than general-purpose models for code tasks
- **Fast Inference**: 6.7B parameters provide good performance without being too slow

### Installation

```bash
# Pull the recommended model
ollama pull deepseek-coder:6.7b
```

### Alternative Models

If you need different performance characteristics:

**Faster (Lower Accuracy)**:
```bash
ollama pull deepseek-coder:1.3b
```
- Faster inference
- Lower accuracy (~75-80%)
- Good for simple code tasks

**Slower (Higher Accuracy)**:
```bash
ollama pull deepseek-coder:33b
```
- Higher accuracy (~88-90%)
- Slower inference
- Better for complex code analysis

### Configuration

The model is configured in `application.yml`:

```yaml
spring:
  ai:
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      chat:
        options:
          model: deepseek-coder:6.7b
          temperature: 0.3  # Lower for more deterministic code analysis
```

### Temperature Settings

- **0.2-0.3**: Recommended for code review (more deterministic, less creative)
- **0.5-0.7**: For code generation (more creative)
- **0.1**: For highly deterministic tasks (may be too rigid)

### Future: Multi-Model Support

For production use, consider:
- **Primary LLM**: Claude 4 Sonnet (via Anthropic API) for critical steps
- **Secondary LLM**: DeepSeek Coder 6.7B (via Ollama) for verification

This hybrid approach provides:
- High accuracy where it matters most (Claude)
- Cost-effective operations for high-volume tasks (DeepSeek)

See `docs/research/llm-models-code-review-2026.md` for detailed research and recommendations.

