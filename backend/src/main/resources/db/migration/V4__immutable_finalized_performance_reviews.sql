CREATE OR REPLACE FUNCTION fn_performance_reviews_immutable_finalized()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.status = 'FINALIZED' THEN
            RAISE EXCEPTION 'Una evaluación debe formalizarse mediante conformidad del empleado'
                USING ERRCODE = 'check_violation';
        END IF;
        RETURN NEW;
    END IF;

    IF OLD.status = 'FINALIZED' THEN
        RAISE EXCEPTION 'La evaluación % ya está formalizada y no puede modificarse ni eliminarse', OLD.id
            USING ERRCODE = 'check_violation';
    END IF;

    IF TG_OP = 'UPDATE'
       AND NEW.status = 'FINALIZED'
       AND (NEW.employee_acknowledged_at IS NULL OR NEW.finalized_at IS NULL) THEN
        RAISE EXCEPTION 'La formalización requiere la fecha de conformidad y de cierre'
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_performance_reviews_immutable_finalized
    BEFORE INSERT OR UPDATE OR DELETE ON performance_reviews
    FOR EACH ROW
    EXECUTE FUNCTION fn_performance_reviews_immutable_finalized();
