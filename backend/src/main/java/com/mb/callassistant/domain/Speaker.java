package com.mb.callassistant.domain;

/**
 * Who is speaking in a transcript turn.
 * <ul>
 *   <li>{@code RM} — Relationship Manager (the agent, our side).</li>
 *   <li>{@code KH} — Khách hàng (the customer).</li>
 * </ul>
 */
public enum Speaker {
    RM,
    KH
}
