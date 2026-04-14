package pt.estga.review.models;

import pt.estga.mark.entities.Mark;
import pt.estga.monument.Monument;

/** Simple holder for resolved entities used by review processors. */
public record ResolutionResult(Mark mark, Monument monument) {}
