-- =====================================================
-- ZIBOTO V3: PUBLIC GALLERIES
-- Migration: V21
-- Description: Shareable file collections
-- Author: Ziboto Team
-- Created: 2026-08-11
-- =====================================================

-- Create galleries table
CREATE TABLE galleries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    slug VARCHAR(255) UNIQUE NOT NULL,
    is_public BOOLEAN DEFAULT TRUE,
    password_protected BOOLEAN DEFAULT FALSE,
    password_hash VARCHAR(255),
    theme VARCHAR(50) DEFAULT 'default',
    layout VARCHAR(50) DEFAULT 'grid',
    view_count BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT galleries_user_id_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create gallery_files junction table
CREATE TABLE gallery_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gallery_id UUID NOT NULL REFERENCES galleries(id) ON DELETE CASCADE,
    file_id UUID NOT NULL REFERENCES file_metadata(id) ON DELETE CASCADE,
    display_order INT DEFAULT 0,
    caption TEXT,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT gallery_files_gallery_id_fk FOREIGN KEY (gallery_id) REFERENCES galleries(id) ON DELETE CASCADE,
    CONSTRAINT gallery_files_file_id_fk FOREIGN KEY (file_id) REFERENCES file_metadata(id) ON DELETE CASCADE,
    CONSTRAINT unique_gallery_file UNIQUE (gallery_id, file_id)
);

-- Create indexes
CREATE INDEX idx_galleries_user_id ON galleries(user_id);
CREATE INDEX idx_galleries_slug ON galleries(slug);
CREATE INDEX idx_galleries_public ON galleries(is_public) WHERE is_public = TRUE;
CREATE INDEX idx_galleries_created_at ON galleries(created_at DESC);

CREATE INDEX idx_gallery_files_gallery_id ON gallery_files(gallery_id);
CREATE INDEX idx_gallery_files_file_id ON gallery_files(file_id);
CREATE INDEX idx_gallery_files_order ON gallery_files(gallery_id, display_order);

-- =====================================================
-- FUNCTIONS
-- =====================================================

-- Get gallery with files
CREATE OR REPLACE FUNCTION get_gallery_with_files(p_gallery_id UUID)
RETURNS TABLE (
    gallery_id UUID,
    title VARCHAR,
    description TEXT,
    slug VARCHAR,
    is_public BOOLEAN,
    theme VARCHAR,
    layout VARCHAR,
    view_count BIGINT,
    file_count BIGINT,
    total_size BIGINT,
    created_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        g.id as gallery_id,
        g.title,
        g.description,
        g.slug,
        g.is_public,
        g.theme,
        g.layout,
        g.view_count,
        COUNT(gf.id)::BIGINT as file_count,
        COALESCE(SUM(fm.file_size), 0)::BIGINT as total_size,
        g.created_at
    FROM galleries g
    LEFT JOIN gallery_files gf ON g.id = gf.gallery_id
    LEFT JOIN file_metadata fm ON gf.file_id = fm.id
    WHERE g.id = p_gallery_id
    GROUP BY g.id;
END;
$$ LANGUAGE plpgsql;

-- Get user galleries
CREATE OR REPLACE FUNCTION get_user_galleries(
    p_user_id BIGINT,
    p_limit INT DEFAULT 50,
    p_offset INT DEFAULT 0
)
RETURNS TABLE (
    gallery_id UUID,
    title VARCHAR,
    slug VARCHAR,
    file_count BIGINT,
    view_count BIGINT,
    is_public BOOLEAN,
    created_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        g.id as gallery_id,
        g.title,
        g.slug,
        COUNT(gf.id)::BIGINT as file_count,
        g.view_count,
        g.is_public,
        g.created_at
    FROM galleries g
    LEFT JOIN gallery_files gf ON g.id = gf.gallery_id
    WHERE g.user_id = p_user_id
    GROUP BY g.id
    ORDER BY g.created_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- Increment gallery view count
CREATE OR REPLACE FUNCTION increment_gallery_views(p_gallery_id UUID)
RETURNS VOID AS $$
BEGIN
    UPDATE galleries
    SET view_count = view_count + 1
    WHERE id = p_gallery_id;
END;
$$ LANGUAGE plpgsql;

-- Generate unique slug
CREATE OR REPLACE FUNCTION generate_gallery_slug(p_title VARCHAR)
RETURNS VARCHAR AS $$
DECLARE
    v_slug VARCHAR;
    v_counter INT := 0;
    v_base_slug VARCHAR;
BEGIN
    -- Convert title to slug format
    v_base_slug := LOWER(REGEXP_REPLACE(p_title, '[^a-zA-Z0-9]+', '-', 'g'));
    v_base_slug := TRIM(BOTH '-' FROM v_base_slug);
    v_slug := v_base_slug;
    
    -- Ensure uniqueness
    WHILE EXISTS (SELECT 1 FROM galleries WHERE slug = v_slug) LOOP
        v_counter := v_counter + 1;
        v_slug := v_base_slug || '-' || v_counter;
    END LOOP;
    
    RETURN v_slug;
END;
$$ LANGUAGE plpgsql;

-- Trigger to update updated_at
CREATE OR REPLACE FUNCTION update_gallery_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER galleries_update_timestamp
BEFORE UPDATE ON galleries
FOR EACH ROW
EXECUTE FUNCTION update_gallery_timestamp();

-- =====================================================
-- COMMENTS
-- =====================================================

COMMENT ON TABLE galleries IS 'Public galleries for sharing file collections';
COMMENT ON COLUMN galleries.slug IS 'URL-friendly unique identifier';
COMMENT ON COLUMN galleries.is_public IS 'Whether gallery is publicly accessible';
COMMENT ON COLUMN galleries.password_protected IS 'Whether gallery requires password';
COMMENT ON COLUMN galleries.theme IS 'Visual theme (default, dark, light, etc.)';
COMMENT ON COLUMN galleries.layout IS 'Layout type (grid, masonry, slideshow)';
COMMENT ON COLUMN galleries.view_count IS 'Number of times gallery has been viewed';

COMMENT ON TABLE gallery_files IS 'Files included in galleries';
COMMENT ON COLUMN gallery_files.display_order IS 'Order in which files are displayed';
COMMENT ON COLUMN gallery_files.caption IS 'Optional caption for file in gallery';

COMMENT ON FUNCTION get_gallery_with_files IS 'Retrieves gallery with file count and size';
COMMENT ON FUNCTION get_user_galleries IS 'Retrieves all galleries for a user';
COMMENT ON FUNCTION increment_gallery_views IS 'Increments view count for a gallery';
COMMENT ON FUNCTION generate_gallery_slug IS 'Generates unique URL slug from title';
