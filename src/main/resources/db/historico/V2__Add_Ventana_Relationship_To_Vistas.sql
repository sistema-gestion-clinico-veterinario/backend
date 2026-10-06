-- Add ventana_id column to vistas table with foreign key relationship
ALTER TABLE vistas ADD COLUMN ventana_id INTEGER;

-- Add foreign key constraint
ALTER TABLE vistas ADD CONSTRAINT fk_vistas_ventana
    FOREIGN KEY (ventana_id) REFERENCES ventanas(id) ON DELETE SET NULL;

-- Create index for faster lookups
CREATE INDEX idx_vistas_ventana_id ON vistas(ventana_id);
