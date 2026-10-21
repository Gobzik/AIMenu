import unittest
from datetime import date
from app import RecommendationRequest, RecommendationService, embedding

class FakeRepository:
    def similarities(self, recipes, preferred):
        return {}

class RecommendationsTest(unittest.TestCase):
    def test_allergies_expiry_and_missing_quantity(self):
        request = RecommendationRequest.model_validate({
            'allergies': ['milk'],
            'pantry': [{'product_id': 'rice', 'quantity': 40}, {'product_id': 'rice', 'quantity': 80, 'expires_at': '2020-01-01'}],
            'recipes': [
                {'id': 'unsafe', 'title': 'Milk', 'ingredients': [{'product_id': 'milk', 'quantity': 1}]},
                {'id': 'rice', 'title': 'Rice', 'ingredients': [{'product_id': 'rice', 'quantity': 100}]}]})
        result = RecommendationService(FakeRepository()).recommend(request, date(2026, 1, 1))
        self.assertEqual([r.recipe_id for r in result.recommendations], ['rice'])
        self.assertEqual(result.recommendations[0].missing_ingredients[0].quantity, 60)

    def test_duplicates_aggregate_and_available_first(self):
        request = RecommendationRequest.model_validate({
            'pantry': [{'product_id': 'rice', 'quantity': 100}],
            'recipes': [
                {'id': 'missing', 'title': 'Large', 'ingredients': [{'product_id': 'rice', 'quantity': 60}, {'product_id': 'rice', 'quantity': 60}]},
                {'id': 'ready', 'title': 'Small', 'ingredients': [{'product_id': 'rice', 'quantity': 100}]}]})
        result = RecommendationService(FakeRepository()).recommend(request)
        self.assertEqual([r.recipe_id for r in result.recommendations], ['ready', 'missing'])
        self.assertEqual(result.recommendations[1].missing_ingredients[0].quantity, 20)

    def test_embedding_deterministic_and_normalized(self):
        self.assertEqual(embedding(['a', 'b']), embedding(['b', 'a', 'a']))
        self.assertAlmostEqual(sum(x*x for x in embedding(['rice'])), 1)

if __name__ == '__main__':
    unittest.main()
