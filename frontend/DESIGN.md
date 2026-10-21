# AIMenu — visual direction

A calm, editorial kitchen journal. Desktop uses a charcoal canvas, fine hairlines, warm ivory text and restrained brass accents. Food photography is the focal point, never decoration behind controls. Mobile uses the same dark canvas and generous touch targets. No gradients, glass effects, floating blobs or pill buttons.

## Type and layout
- Headlines, body and controls: local Manrope. Strong contrast between large titles and small uppercase section labels.
- Desktop content max width 1360px; 12-column implied grid; 28px gutters. Mobile: 20px outer padding, single column.
- Palette: charcoal #171715, panels #22221f, border #414039, ivory #f3efe5, muted #a9a69e, brass #b89765.
- Controls: rectangular with 4px radius and clear focus outline. Primary action uses ivory on charcoal; secondary actions are outlined.

## Screens
Home: photographic hero, context links, featured recipes. Catalog: compact photography cards with stock status. Recipe: image, exact ingredient quantities, shopping deficits, ordered method. Pantry: batch rows with Russian date input and expiry through 2500. Recommendations: fresh stock, ready meals, missing items and allergy exclusions. Account: login, registration, family details and card-based preference selection. Shopping: focused per-recipe checklist with quantity and purchase entry.

## Interaction
Primary navigation stays visible desktop, bottom navigation on phones. Opening a recipe preserves its context from catalog or recommendations. Authentication is requested only for stock and profile actions. API errors remain visible beside the action. Loading and empty states use plain text, not fake data.
