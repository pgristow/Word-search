-- Insert initial categories
INSERT INTO categories (id, name, description, unlock_requirement_type, unlock_requirement_value, display_order, is_active)
VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'Animals', 'Common and exotic animals from around the world', 'NONE', 0, 1, true),
    ('550e8400-e29b-41d4-a716-446655440002', 'Food', 'Delicious foods and ingredients', 'WORDS', 50, 2, true),
    ('550e8400-e29b-41d4-a716-446655440003', 'Sports', 'Popular sports and activities', 'LEVEL', 10, 3, true),
    ('550e8400-e29b-41d4-a716-446655440004', 'Science', 'Scientific terms and concepts', 'WORDS', 200, 4, true),
    ('550e8400-e29b-41d4-a716-446655440005', 'Nature', 'Plants, weather, and natural phenomena', 'LEVEL', 20, 5, true),
    ('550e8400-e29b-41d4-a716-446655440006', 'Technology', 'Modern tech and gadgets', 'LEVEL', 30, 6, true);
