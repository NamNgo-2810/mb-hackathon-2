# Stakeholder Map — CRM VoIP Call Assistant

## Key Stakeholders and Interests

| Stakeholder | Interest | Decision / Influence | Source |
|-------------|----------|----------------------|--------|
| Relationship Manager (RM) | Places calls from the web; wants automatic call summary, form-fill, and a next-sell suggestion so after-call work is fast | Primary user / influencer | [Q2] |
| Business Analyst (BA) | Owns the Figma design the UI must match ("stunning", on-design) | Influencer (design authority) | [Q1][desc] |
| Hackathon builder / developer | Delivers the Figma-matched UI, DB design, Vietnamese speech-to-text, and Spring Boot backend in one day | Decision-maker (build choices the user delegated) | [desc][Q1] |
| Customer (call recipient) | Receives the call; identified in the demo by FreeSWITCH default users | Influencer (not a system user) | [Q4][desc] |

## Decision-Makers vs. Influencers

- **Decision-makers**: the hackathon builder makes the technical build decisions
  the user delegated (backend, DB, speech-to-text provider, UI implementation)
  [desc].
- **Influencers**: the RM (shapes the needed outputs and workflow) [Q2] and the
  BA (owns the design the UI must match) [Q1][desc].

## Communication Requirements

| Requirement | Detail | Source |
|-------------|--------|--------|
| Design handoff | UI must closely match the BA-provided Figma design (https://www.figma.com/design/ObEPaidO5YFTYB5kxk5hju/Untitled?node-id=0-1) | [Q1][Q5] |
| Demo readiness | The end-to-end flow is shown in a single hackathon demo | [Q1] |

## Assumptions & Open Questions

None.
