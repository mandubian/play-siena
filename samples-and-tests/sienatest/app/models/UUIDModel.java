package models;

import play.modules.siena.Model;
import siena.Generator;
import siena.Id;
import siena.Table;
import siena.embed.EmbeddedMap;

@Table("uuid_models")
public class UUIDModel extends Model{
    @Id(Generator.UUID)
    public String id;
    
    public String 	alpha;
    public short	beta;
    
    public String toString() {
    	return id + " " + alpha + " " + beta;
    }
}
