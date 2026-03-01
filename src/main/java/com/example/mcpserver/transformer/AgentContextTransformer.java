package com.example.mcpserver.transformer;

/**
 * Base interface for transformers that convert raw domain objects into agent-safe contexts.
 * <p>
 * Transformers serve as Layer 1 of the data processing pipeline, responsible for:
 * <ul>
 *   <li><strong>PII Stripping</strong>: Removing personally identifiable information
 *       (SSN, DOB, addresses, personal contacts)</li>
 *   <li><strong>Internal Data Removal</strong>: Removing internal IDs, partition keys,
 *       ETags, budget codes, and other operational metadata</li>
 *   <li><strong>Data Enrichment</strong>: Computing derived fields that are useful for
 *       AI agents (e.g., SLA status, average assessment scores)</li>
 *   <li><strong>Projection</strong>: Creating focused views with only relevant data
 *       for agent consumption</li>
 * </ul>
 * </p>
 * <p>
 * Transformers ensure that AI agents receive clean, focused, privacy-compliant data
 * without direct access to sensitive enterprise systems or PII.
 * </p>
 *
 * @param <T> the source type (raw domain object from enterprise systems)
 * @param <R> the result type (agent-safe context object)
 * @since 1.0
 */
public interface AgentContextTransformer<T, R> {

    /**
     * Transforms a raw domain object into an agent-safe context object.
     * <p>
     * Implementations must:
     * <ul>
     *   <li>Remove all PII and sensitive data</li>
     *   <li>Remove internal metadata and operational fields</li>
     *   <li>Compute derived fields useful for agents</li>
     *   <li>Handle null inputs gracefully</li>
     *   <li>Be deterministic and idempotent</li>
     * </ul>
     * </p>
     *
     * @param source the raw domain object to transform (may be null)
     * @return the transformed agent-safe context, or null if source is null
     */
    R transform(T source);
}
